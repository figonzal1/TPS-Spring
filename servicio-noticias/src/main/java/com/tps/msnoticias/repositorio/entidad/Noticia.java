package com.tps.msnoticias.repositorio.entidad;

import javax.persistence.*;

@Entity
@Table(name = "noticia")
public class Noticia {
    @Id
    @GeneratedValue
    @Column(name = "id")
    private int id;

    @Column(name = "titular")
    private String titular;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "autor")
    private String autor;

    @Column(name = "url")
    private String url;

    @Column(name = "fuente")
    private String fuente;

    @Column(name = "id_categoria")
    private int id_categoria;

    public Noticia() {
    }

    public Noticia(String titular, String descripcion, String autor, String url, String fuente, int id_categoria) {
        this.titular = titular;
        this.descripcion = descripcion;
        this.autor = autor;
        this.url = url;
        this.fuente = fuente;
        this.id_categoria = id_categoria;
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

    public int getId_categoria() {
        return id_categoria;
    }

    public void setId_categoria(int id_categoria) {
        this.id_categoria = id_categoria;
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
                ", id_categoria=" + id_categoria +
                '}';
    }
}