package com.tps.msnoticias.mensajeria;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.tps.msnoticias.dominio.CategoriaNoticiaVO;
import com.tps.msnoticias.dominio.FuenteNoticiaVO;
import com.tps.msnoticias.dominio.NoticiaRoot;
import com.tps.msnoticias.servicio.NoticiaService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

@Component("msgAdapter")
public class MsgImpl implements Msg {

    //NOTICIA
    private static final String EXCHANGE_NAME = "noticia_exchange";

    private static final String ROUTE_KEY_CREATE = "noticia.crear";
    private static final String ROUTE_KEY_EDIT = "noticia.editar";
    private static final String ROUTE_KEY_DELETE = "noticia.eliminar";

    private static final String ROUTE_KEY_LIST = "noticia.lista";

    private static final String QUEUE_REQUEST_CUD = "noticia_request_cud";
    private static final String QUEUE_REQUEST_LIST = "noticia_request_list";

    //CATEGORIA
    private static final String EXCHANGE_NAME_CAT = "categoria_exchange";
    private static final String ROUTE_KEY_LIST_CAT = "categoria.lista";
    private static final String ROUTE_KEY_LIST_CAT_SUBS = "noticia.lista.suscritos";
    private static final String QUEUE_REQUEST_LIST_CAT = "noticia_susc_categoria";
    private String jsonCategoriaList;

    private static final Log LOGGER = LogFactory.getLog(MsgImpl.class);

    private final NoticiaService noticiaService;

    public MsgImpl(@Qualifier("noticiaService") NoticiaService noticiaService) {
        this.noticiaService = noticiaService;
    }

    /**
     * Proceso de mensajeria encargado de CREAR, ACTUALIZAR y ELIMINAR noticias.
     * (Suscripcion)
     */
    @Override
    public void procesarCUD() {
        try {
            Channel channel = RabbitMQ.getChannel();
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");

            String receiver_queue = channel.queueDeclare(QUEUE_REQUEST_CUD, true, false, false, null).getQueue();

            LOGGER.info("Creando queue: " + receiver_queue);

            //Configuracion de rutas
            channel.queueBind(receiver_queue, EXCHANGE_NAME, ROUTE_KEY_CREATE);
            channel.queueBind(receiver_queue, EXCHANGE_NAME, ROUTE_KEY_EDIT);
            channel.queueBind(receiver_queue, EXCHANGE_NAME, ROUTE_KEY_DELETE);

            LOGGER.info("[*] Esperando por solicitudes de (Creacion - Edicion - Eliminacion) de noticias. Para salir " +
                    "presiona CTRL+C");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {

                String json = new String(delivery.getBody(), StandardCharsets.UTF_8);
                JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();

                switch (delivery.getEnvelope().getRoutingKey()) {

                    //Solicitudes de creacion de noticias
                    case ROUTE_KEY_CREATE: {

                        FuenteNoticiaVO fuenteNoticiaVO = new FuenteNoticiaVO(jsonObject.get("fuente").getAsString());
                        CategoriaNoticiaVO categoriaNoticiaVO =
                                new CategoriaNoticiaVO(jsonObject.get("id_categoria").getAsInt());

                        NoticiaRoot noticiaRoot = new NoticiaRoot(jsonObject.get("titular").getAsString(),
                                jsonObject.get("descripcion").getAsString(), jsonObject.get("autor").getAsString(),
                                jsonObject.get("url").getAsString(), fuenteNoticiaVO, categoriaNoticiaVO);

                        LOGGER.info("[x] Recibido por queue '" + receiver_queue + "' -> " + noticiaRoot.toString());

                        //Persistir noticia
                        noticiaService.agregar(noticiaRoot);

                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                        break;
                    }

                    //Solicitudes de edicion de noticias
                    case ROUTE_KEY_EDIT: {

                        FuenteNoticiaVO fuenteNoticiaVO = new FuenteNoticiaVO(jsonObject.get("fuente").getAsString());
                        CategoriaNoticiaVO categoriaNoticiaVO =
                                new CategoriaNoticiaVO(jsonObject.get("id_categoria").getAsInt());

                        NoticiaRoot noticiaRoot = new NoticiaRoot(jsonObject.get("id").getAsInt(), jsonObject.get(
                                "titular").getAsString(),
                                jsonObject.get("descripcion").getAsString(), jsonObject.get("autor").getAsString(),
                                jsonObject.get("url").getAsString(), fuenteNoticiaVO, categoriaNoticiaVO);

                        LOGGER.info("[x] Recibido por queue '" + receiver_queue + "' -> " + noticiaRoot.toString());

                        noticiaService.editar(noticiaRoot);

                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                        break;
                    }

                    //Solicitudes de eliminacion de noticias
                    case ROUTE_KEY_DELETE: {

                        NoticiaRoot noticiaRoot = new NoticiaRoot(jsonObject.get("id").getAsInt());

                        LOGGER.info("[x] Recibido por queue '" + receiver_queue + "' -> " + noticiaRoot.toString());

                        //eliminar noticia
                        noticiaService.eliminar(noticiaRoot);

                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                        break;
                    }
                }
            };

            boolean autoAck = false;
            channel.basicConsume(receiver_queue, autoAck, deliverCallback, (consumerTag) -> {
            });

        } catch (NoSuchAlgorithmException | KeyManagementException | URISyntaxException | IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    /**
     * Proceso de mensajeria encargado de enviar listado de noticias.
     * (Request-Response Sincronico a solicitudes desde Apigateway)
     */
    @Override
    public void procesarListadoNoticias() {

        try {
            Channel channel = RabbitMQ.getChannel();
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");
            String receiver_queue = channel.queueDeclare(QUEUE_REQUEST_LIST, false, false, false, null).getQueue();

            //CONFIGURACION DE RUTAS
            channel.queueBind(receiver_queue, EXCHANGE_NAME, ROUTE_KEY_LIST);

            LOGGER.info("Creando queue: " + receiver_queue);
            LOGGER.info("[*] Esperando por solicitudes de lista noticias. Para salir presiona CTRL+C");

            Object monitor = new Object();

            //RECEPCION DE SOLICITUDES
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {

                String json = new String(delivery.getBody(), StandardCharsets.UTF_8);
                LOGGER.info("[x] Solicitud de listado de noticias desde '" + json + "'");

                AMQP.BasicProperties reply_props = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(delivery.getProperties().getCorrelationId())
                        .build();

                //SOLICITAR LISTADO DE CATEGORIAS A MSCATEGORIA

                List<NoticiaRoot> noticias = noticiaService.obtenerLista(jsonCategoriaList);

                byte[] data = (new Gson().toJson(noticias).getBytes(StandardCharsets.UTF_8));

                //Enviarlo por cola unica (reply_to)
                channel.basicPublish("", delivery.getProperties().getReplyTo(), reply_props, data);

                LOGGER.info("[x] Enviando por queue '" + delivery.getProperties().getReplyTo() + "' -> " + noticias
                        .toString());

                synchronized (monitor) {
                    monitor.notify();
                }
            };

            //En espera de solicitudes
            channel.basicConsume(receiver_queue, true, deliverCallback, (consumerTag) -> {
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

    /**
     * Proceso de mensajeria encargado de suscribirse y recibir actualizaciones de listado de categorias
     * enviadas desde Mscategoria
     * (Suscripcion)
     */
    @Override
    public void procesarListadoCategorias() {

        //Solicitar datos en primera partida
        solicitarListadoCategorias();

        try {
            Channel channel = RabbitMQ.getChannel();

            channel.exchangeDeclare(EXCHANGE_NAME_CAT, "direct");

            String receiver_queue = channel.queueDeclare(QUEUE_REQUEST_LIST_CAT, false, false, false, null).getQueue();

            LOGGER.info("Creando queue: " + receiver_queue);

            //Configuracion de rutas
            channel.queueBind(receiver_queue, EXCHANGE_NAME_CAT, ROUTE_KEY_LIST_CAT_SUBS);

            LOGGER.info("[*] Esperando por actualizaciones de categorias. Para salir presiona CTRL+C");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {

                String json = new String(delivery.getBody(), StandardCharsets.UTF_8);

                LOGGER.info("[x] Recibido por queue '" + receiver_queue + "' -> " + json);

                jsonCategoriaList = json;

            };
            channel.basicConsume(receiver_queue, true, deliverCallback, (consumerTag) -> {
            });
        } catch (IOException | NoSuchAlgorithmException | URISyntaxException | TimeoutException | KeyManagementException e) {
            e.printStackTrace();
        }
    }

    /**
     * Funcion encargada de solicitar listado de categorias a MsCategoria cuando
     * MsNoticia se encuentra en partida en frio.
     * (Request-response) Hacia mscategoria
     */
    private void solicitarListadoCategorias() {

        try {
            Channel channel = RabbitMQ.getChannel();
            channel.exchangeDeclare(EXCHANGE_NAME_CAT, "direct");

            String correlation_id = UUID.randomUUID().toString();

            //Queue de respuesta (Queue aleatoria)
            String receiver_queue = channel.queueDeclare().getQueue();
            LOGGER.info("Creando queue receptora: " + receiver_queue);

            AMQP.BasicProperties properties = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(correlation_id)
                    .replyTo(receiver_queue)
                    .build();

            String consumer = "msnoticia";
            byte[] data = consumer.getBytes(StandardCharsets.UTF_8);

            //Publicacion hacia exchange con ruta adecuada
            channel.basicPublish(EXCHANGE_NAME_CAT, ROUTE_KEY_LIST_CAT, properties, data);
            LOGGER.info("[x] Solicitando lista categorias por exchange '" + EXCHANGE_NAME_CAT + "' por ruta '" + ROUTE_KEY_LIST_CAT + "'");

            //RECEPCION DE MENSAJES DESDE MSCATEGORIA
            BlockingQueue<String> response = new ArrayBlockingQueue<>(1);

            String ctag = channel.basicConsume(receiver_queue, true, (consumerTag, delivery) -> {

                if (delivery.getProperties().getCorrelationId().equals(correlation_id)) {
                    response.offer(new String(delivery.getBody(), StandardCharsets.UTF_8));
                }

            }, consumerTag -> {
            });

            jsonCategoriaList = response.take();
            channel.basicCancel(ctag);

            LOGGER.info("[x] Recibido por queue '" + receiver_queue + "' -> " + jsonCategoriaList);

        } catch (IOException | NoSuchAlgorithmException | URISyntaxException | TimeoutException | InterruptedException | KeyManagementException e) {
            e.printStackTrace();
        }
    }

}
