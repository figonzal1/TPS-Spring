package com.ts.msfavoritos.repositorio.entidad;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**Entidad de favorito para el jpa**/
@Entity
@Table(name = "favorito")
public class Favorito {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private int id;

  @Column(name = "id_noticia")
  private int idNoticia;

  @Column(name = "id_usuario")
  private int idUsuario;

  @Column(name = "fecha_favorito")
  private String fechaFavorito;

  public Favorito() {
  }

  /**constructor de favorito **/
  public Favorito(int idNoticia, int idUsuario, String fechaFavorito) {
    this.idNoticia = idNoticia;
    this.idUsuario = idUsuario;
    this.fechaFavorito = fechaFavorito;
  }

  //geters y setters
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getIdNoticia() {
    return idNoticia;
  }

  public void setIdNoticia(int idNoticia) {
    this.idNoticia = idNoticia;
  }

  public int getIdUsuario() {
    return idUsuario;
  }

  public void setIdUsuario(int idUsuario) {
    this.idUsuario = idUsuario;
  }

  public String getFechaFavorito() {
    return fechaFavorito;
  }

  public void setFechaFavorito(String fechaFavorito) {
    this.fechaFavorito = fechaFavorito;
  }

  @Override
  public String toString() {
    return "Favorito{" +
        "id=" + id +
        ", idNoticia=" + idNoticia +
        ", idUsuario=" + idUsuario +
        ", fechaFavorito='" + fechaFavorito + '\'' +
        '}';
  }
}
