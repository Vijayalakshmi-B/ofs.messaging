package ofs.messaging;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hibernate.Session;
import org.hibernate.Transaction;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.deps.com.fasterxml.jackson.databind.Module.SetupContext;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ofs.messaging.Client.Channel;
import ofs.messaging.Client.Impl.MessagePublisher;
import ofs.messaging.Client.Impl.RabbitMQChannel;
import ofs.messaging.Client.Impl.RabbitMQClient;
import ofs.messaging.Client.Impl.RabbitMQConnection;
import ofs.messaging.Models.ClientRegistration;
import ofs.messaging.Models.Routing;
import ofs.messaging.Persistence.PersistenceManager;


public class testExistingPublishingClient {

  public static final Logger log = LoggerFactory
      .getLogger(testPublishingWithNewClientRegistration.class);
  public static int i = 0;

  public static void main(String[] args) throws NamingException {

    // PersistenceManager.cleanUp();

    // creating a context - use the jndi proeprties file for url and initial context factory
    Context ctx = new InitialContext();
    RabbitMQConnection con = (RabbitMQConnection) ctx.lookup("RabbitMQConnection");

    log.info("start of producer 1");

    Channel channelObject = null;
    Message msg = null;
    boolean isRedundant = true;

    try {



      // getting an event id as we want to publish messages for those events
      String EventId = PersistenceManager.listEvents().get(6).getEventId(); // 6 is dispatch in our
                                                                            // testPublishingWithNewClientRegistration
      // 56313dfd-23a1-43ca-bbe0-ee994c4fb851 // db
      log.debug("Event id is:" + EventId);


      String clientId =
          PersistenceManager.getRegistrationClientIdFromOtherDetails("GMO OMS", "IGHS5");
      log.debug("Client id is :" + clientId);


      final String exchangeId = PersistenceManager.getExangeIdFromClientId(clientId);

      if (exchangeId.isEmpty()) {
        throw new Exception("Exchange Id shouldnt be null. check the client id");

      }
      log.debug("Exchange Id is :" + exchangeId);

      String routingKey = Routing.getRoutingKey(clientId);
      log.debug("Routing is: " + routingKey);

      // System.exit(0);

      RabbitMQClient clientNew =
          new RabbitMQClient().getInstance(ClientRegistration.getClient(clientId));


      // creating a channel to connect and declaring the exchange
      channelObject = new RabbitMQChannel(con.connect());
      channelObject.createChannel();

      channelObject.exchangeDeclare(exchangeId);

      Path path = Paths.get("test.json");
      byte[] data = null;
      data = Files.readAllBytes(path);



      long startTime = System.currentTimeMillis();


      // for (int i = 0; i < 10000; i++) {
      for (;;) {

        Payload payload = new Payload();
        payload.setPayLoadFormat(PayloadFormat.JSON);
        payload.setData(new String(data));
        isRedundant = isRedundant ? false : true;
        msg = new Message(clientId, payload, isRedundant);
        log.debug(new Boolean(isRedundant).toString());

        MessagePublisher mp = new MessagePublisher(channelObject, exchangeId, routingKey, msg);
        clientNew.publish(mp);

      }

      // clientNew.waitForScheduledTasksToComplete(20, TimeUnit.SECONDS);
      //
      // log.debug("Completed...");
      //
      // long endTime = System.currentTimeMillis();
      // log.debug("Total duration is:" + Long.toString((endTime - startTime)));
      // channelObject.close();
      // con.close();

    } catch (Exception e) {
      log.error("App failed", e);

    } finally {
      System.exit(0);
    }
  }
}
