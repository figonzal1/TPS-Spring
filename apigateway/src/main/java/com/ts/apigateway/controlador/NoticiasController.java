package com.ts.apigateway.controlador;

import com.ts.apigateway.mensajeria.NoticiaMsgAdapter;
import com.ts.apigateway.modelo.Noticia;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

@RestController
public class NoticiasController {
    private static final String ROUTE_KEY_CREATE = "noticia.crear";
    private static final String ROUTE_KEY_EDIT = "noticia.editar";
    private static final String ROUTE_KEY_DELETE = "noticia.eliminar";
    private static final Log LOGGER = LogFactory.getLog(NoticiasController.class);

    private final NoticiaMsgAdapter noticiaMsgAdapter;

    public NoticiasController(@Qualifier("noticiaMsgAdapter") NoticiaMsgAdapter noticiaMsgAdapter) {
        this.noticiaMsgAdapter = noticiaMsgAdapter;
    }

    @PostMapping("/noticia/agregar")
    public Noticia addNoticia(@RequestBody Noticia noticia) {
        LOGGER.info("Recibido desde post: " + noticia.toString());
        noticiaMsgAdapter.send(noticia,ROUTE_KEY_CREATE);
        return noticia;
    }

    @DeleteMapping("/noticia/eliminar")
    public Noticia eliminar(@RequestBody Noticia noticia){
        LOGGER.info("Recibido desde delete: " + noticia.toString());
        noticiaMsgAdapter.send(noticia,ROUTE_KEY_DELETE);
        return noticia;
    }

    @PutMapping("/noticia/editar")
    public Noticia editar(@RequestBody Noticia noticia){
        LOGGER.info("Recibido desde editar: " + noticia.toString());
        noticiaMsgAdapter.send(noticia,ROUTE_KEY_EDIT);
        return noticia;
    }
}
