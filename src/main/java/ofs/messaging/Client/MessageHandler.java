package ofs.messaging.Client;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import ofs.messaging.Message;
import ofs.messaging.test;
import ofs.messaging.Client.Exceptions.MessageDeliveryFailedException;
import ofs.messaging.Client.Impl.RabbitMQChannel;
import ofs.messaging.Client.Impl.RedundancyManager;

import com.couchbase.client.CouchbaseClient;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.tesco.ofs.platform.trace.logger.OFSPlatformLogger;

public abstract class MessageHandler extends DefaultConsumer implements Handler {
	public static final OFSPlatformLogger log = OFSPlatformLogger.getLogger(MessageHandler.class);
	private static CouchbaseClient cbClient = null;
	private RabbitMQChannel channel;

	public MessageHandler(ofs.messaging.Client.Channel channelObject) {
		super(((RabbitMQChannel) channelObject).getChannel());
		this.channel = (RabbitMQChannel) channelObject;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ofs.messaging.Client.Handler#doProcess(byte[])
	 */
	public abstract String doProcess(byte[] body);

	/*
	 * (non-Javadoc)
	 * 
	 * @see ofs.messaging.Client.Handler#handleDelivery(java.lang.String,
	 * com.rabbitmq.client.Envelope, com.rabbitmq.client.AMQP.BasicProperties, byte[])
	 */
	@Override
	public void handleDelivery(String consumerTag, Envelope envelope,
			AMQP.BasicProperties properties, byte[] body) throws IOException {

		final long msgTag = envelope.getDeliveryTag();
		String msgId = "";
		try {
			msgId = doProcess(body);

			channel.getChannel().basicAck(msgTag, false);
			removeMsg(msgId);
		} catch (IOException e) {

			log.error("Processing or Ack failed", e);
			throw new MessageDeliveryFailedException("Processing/ or Ack Failed", e);
		} catch (InterruptedException e) {
			log.error("Processing or Ack failed", e);
			throw new MessageDeliveryFailedException("Processing/ or Ack Failed", e);
		} catch (ExecutionException e) {

			log.error("Processing or Ack failed", e);
			throw new MessageDeliveryFailedException("Processing/ or Ack Failed", e);
		}

	}

	private void removeMsg(String msgId) throws InterruptedException, ExecutionException {

		if (cbClient == null) {
			cbClient = RedundancyManager.getInstance();
		}

		cbClient.delete(msgId).get();
		log.debug("Message removed successfully :" + msgId);
	}
}
