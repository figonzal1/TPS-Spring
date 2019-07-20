package com.ts.apigateway.modelo;

public class Noticia {

    private int id;
    private String titular;
    private String descripcion;
    private String autor;
    private String url;
    private String fuente;
    private String categoria;

    public Noticia() {
    }

    public Noticia(int id, String titular, String descripcion, String autor, String url, String fuente,
                   String categoria) {
        this.id = id;
        this.titular = titular;
        this.descripcion = descripcion;
        this.autor = autor;
        this.url = url;
        this.fuente = fuente;
        this.categoria = categoria;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitular() {
        return titular;
    }

    public void setTitular(String titular) {
        this.titular = titular;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFuente() {
        return fuente;
    }

    public void setFuente(String fuente) {
        this.fuente = fuente;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    @Override
    public String toString() {
        return "Noticia{" +
                "id=" + id +
                ", titular='" + titular + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", autor='" + autor + '\'' +
                ", url='" + url + '\'' +
                ", fuente='" + fuente + '\'' +
                ", categoria='" + categoria + '\'' +
                '}';
    }
}
