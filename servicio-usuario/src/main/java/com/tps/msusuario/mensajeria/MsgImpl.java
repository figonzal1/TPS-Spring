package com.tps.msusuario.mensajeria;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.tps.msusuario.dominio.EstadoUsuarioVO;
import com.tps.msusuario.dominio.NombreUsuarioVO;
import com.tps.msusuario.dominio.UsuarioRoot;
import com.tps.msusuario.servicio.UsuarioService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Component("msgAdapter")
public class MsgImpl implements Msg {

    private static final String EXCHANGE_NAME = "usuario_exchange";

    private static final String ROUTE_KEY_CREATE = "usuario.crear";
    private static final String ROUTE_KEY_DELETE = "usuario.eliminar";
    private static final String ROUTE_KEY_EDIT = "usuario.editar";
    private static final String ROUTE_KEY_LOGIN = "usuario.login";

    private static final String QUEUE_REQUEST_CUD = "usuario_request_cud";
    private static final String QUEUE_REQUEST_LOGIN = "usuario_request_login";

    private static final Log LOGGER = LogFactory.getLog(MsgImpl.class);

    private final UsuarioService usuarioService;

    public MsgImpl(@Qualifier("usuarioService") UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Proceso de mensajeria encargado de CREAR, ACTUALIZAR y ELIMINAR usuarios.
     */
    @Override
    public void processCUD() {

        try {
            Channel channel = RabbitMQ.getChannel();

            channel.exchangeDeclare(EXCHANGE_NAME, "direct");

            String receiver_queue = channel.queueDeclare(QUEUE_REQUEST_CUD, true, false, false, null).getQueue();

            LOGGER.info("Creando queue: " + receiver_queue);

            channel.queueBind(receiver_queue, EXCHANGE_NAME, ROUTE_KEY_CREATE);
            channel.queueBind(receiver_queue, EXCHANGE_NAME, ROUTE_KEY_DELETE);
            channel.queueBind(receiver_queue, EXCHANGE_NAME, ROUTE_KEY_EDIT);

            LOGGER.info("[*] Esperando por solicitudes de creacion de usuarios. Para salir presiona CTRL+C");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {

                String json = new String(delivery.getBody());
                JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();

                //Solicitud de creacion de usuario
                switch (delivery.getEnvelope().getRoutingKey()) {

                    case ROUTE_KEY_CREATE:

                        NombreUsuarioVO nombreUsuarioVO = new NombreUsuarioVO(jsonObject.get("username").getAsString());
                        EstadoUsuarioVO estadoUsuarioVO = new EstadoUsuarioVO(jsonObject.get("estado").getAsString());

                        //Construccion agregado
                        UsuarioRoot usuarioRoot = new UsuarioRoot(nombreUsuarioVO,
                                jsonObject.get("email").getAsString(),
                                jsonObject.get("password").getAsString(), estadoUsuarioVO);

                        LOGGER.info("[x] Recibido por queue '" + receiver_queue + "' -> " + usuarioRoot.toString());

                        usuarioService.agregarUsuario(usuarioRoot);

                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                        break;

                    //Solicitudes de editar usuario
                    case ROUTE_KEY_EDIT:

                        NombreUsuarioVO nombreUsuarioVO2 =
                                new NombreUsuarioVO(jsonObject.get("username").getAsString());
                        EstadoUsuarioVO estadoUsuarioVO2 = new EstadoUsuarioVO(jsonObject.get("estado").getAsString());

                        //Construccion agregado
                        UsuarioRoot usuarioRoot2 = new UsuarioRoot(jsonObject.get("id").getAsInt(), nombreUsuarioVO2,
                                jsonObject.get("email").getAsString(),
                                jsonObject.get("password").getAsString(), estadoUsuarioVO2);

                        LOGGER.info("[x] Recibido por queue '" + receiver_queue + "' -> " + usuarioRoot2.toString());

                        usuarioService.editarUsuario(usuarioRoot2);

                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                        break;

                    //Solicitudes de eliminar usuarios
                    case ROUTE_KEY_DELETE:

                        //Construccion agregado
                        UsuarioRoot usuarioRoot3 = new UsuarioRoot(jsonObject.get("id").getAsInt());

                        LOGGER.info("[x] Recibido por queue '" + receiver_queue + "' -> " + usuarioRoot3.toString());

                        //Eliminar usuario
                        usuarioService.eliminarUsuario(usuarioRoot3);

                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                        break;
                }
            };

            boolean autoAck = false;
            channel.basicConsume(receiver_queue, autoAck, deliverCallback, (consumerTag) -> {
            });
        } catch (IOException | NoSuchAlgorithmException | URISyntaxException | TimeoutException | KeyManagementException e) {
            e.printStackTrace();
        }
    }

    /**
     * Proceso de mensajeria encargado del logear usuarios en el sistema
     */
    @Override
    public void processLogin() {
        //RPC
        try {
            Channel channel = RabbitMQ.getChannel();

            channel.exchangeDeclare(EXCHANGE_NAME, "direct");

            String receiver_queue = channel.queueDeclare(QUEUE_REQUEST_LOGIN, false, false, false, null).getQueue();

            LOGGER.info("Creando queue: " + receiver_queue);

            channel.queueBind(receiver_queue, EXCHANGE_NAME, ROUTE_KEY_LOGIN);

            LOGGER.info("[*] Esperando por solicitudes de login. Para salir presiona CTRL+C");

            Object monitor = new Object();

            //RECEPCION DE SOLICITUDES
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {

                AMQP.BasicProperties reply_props = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(delivery.getProperties().getCorrelationId())
                        .build();

                String json_received = new String(delivery.getBody(), StandardCharsets.UTF_8);
                UsuarioRoot usuarioRoot = new Gson().fromJson(json_received, UsuarioRoot.class);

                LOGGER.info("[x] Recibido por queue '" + receiver_queue + "' -> " + usuarioRoot.toString());
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                //Realizar login
                Map<String, String> result = usuarioService.loginUsuario(usuarioRoot);
                String json_result = new Gson().toJson(result);

                //Enviarlo por cola unica (reply_to)
                channel.basicPublish("", delivery.getProperties().getReplyTo(), reply_props,
                        json_result.getBytes(StandardCharsets.UTF_8));

                LOGGER.info("[x] Enviando por queue '" + delivery.getProperties().getReplyTo() + "' -> " + json_result);

                synchronized (monitor) {
                    monitor.notify();
                }
            };

            //En espera de solicitudes
            channel.basicConsume(receiver_queue, false, deliverCallback, (consumerTag) -> {
            });

            while (true) {
                synchronized (monitor) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        } catch (IOException | NoSuchAlgorithmException | URISyntaxException | TimeoutException | KeyManagementException e) {
            e.printStackTrace();
        }
    }
}