package io.swiftwallet.odin.core.bootstrap.discovery.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.swiftwallet.odin.core.bootstrap.MicroServiceProperties;
import io.swiftwallet.odin.core.bootstrap.discovery.ServiceDiscoveryProperties;
import io.swiftwallet.odin.core.lb.OdinServer;
import io.swiftwallet.odin.core.services.server.EmbeddedServerProperties;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

/**
 * Created by gibugeorge on 24/12/2016.
 */
@Configuration
@EnableConfigurationProperties({ServiceDiscoveryProperties.class, MicroServiceProperties.class, EmbeddedServerProperties.class})
@ConditionalOnProperty(prefix = "service-discovery", value = "register", havingValue = "true", matchIfMissing = true)
public class SericeRegistrationConfiguration {


    private static final Logger LOGGER = LoggerFactory.getLogger(SericeRegistrationConfiguration.class);

    @Autowired
    private ConfigurableBeanFactory beanFactory;

    @Autowired
    private CuratorFramework curatorFramework;

    @Autowired
    private ServiceDiscoveryProperties serviceDiscoveryProperties;

    @Autowired
    private MicroServiceProperties microServiceProperties;

    @Autowired
    private EmbeddedServerProperties embeddedServerProperties;

    @Autowired
    private ObjectMapper objectMapper;

    private String registeredPath;


    @PostConstruct
    public void register() throws Exception {

        final String servicePath = this.serviceDiscoveryProperties.getRoot() + "/" + this.microServiceProperties.getName();
        final Stat isRegistered = curatorFramework.checkExists().creatingParentContainersIfNeeded().forPath(servicePath);
        if (isRegistered == null) {
            curatorFramework.createContainers(servicePath);
        }
        registeredPath = servicePath + "/" + this.microServiceProperties.getId();
        if (curatorFramework.checkExists().forPath(registeredPath) != null) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Un-registering the service {}", registeredPath);
            }
            unRegister();
        }
        String address = null;
        Collection<InetAddress> ips = getAllLocalIPs();
        if (ips.size() > 0) {
            address = ips.iterator().next().getHostAddress();   // default to the first address
        }
        final int serverPort = embeddedServerProperties.getPort();

        final OdinServer odinServer = new OdinServer(address, serverPort);
        odinServer.setReadyToServe(true);
        odinServer.setAlive(true);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Registering the service {}", registeredPath);
        }
        curatorFramework.create().forPath(registeredPath, objectMapper.writeValueAsBytes(odinServer));
        beanFactory.registerSingleton("odinSerer", odinServer);

    }

    @PreDestroy
    public void unRegister() throws Exception {
        this.curatorFramework.delete().forPath(registeredPath);

    }

    private static Collection<InetAddress> getAllLocalIPs() throws SocketException {
        final ArrayList listAdr = Lists.newArrayList();
        final Enumeration nifs = NetworkInterface.getNetworkInterfaces();
        if (nifs == null) {
            return listAdr;
        } else {
            while (nifs.hasMoreElements()) {
                final NetworkInterface nif = (NetworkInterface) nifs.nextElement();
                final Enumeration adrs = nif.getInetAddresses();
                while (adrs.hasMoreElements()) {
                    final InetAddress adr = (InetAddress) adrs.nextElement();
                    if ((adr != null) && adr instanceof Inet4Address && (!adr.isLoopbackAddress() && (nif.isPointToPoint() || !adr.isLinkLocalAddress()))) {
                        listAdr.add(adr);
                    }
                }
            }
        }

        return listAdr;
    }

}