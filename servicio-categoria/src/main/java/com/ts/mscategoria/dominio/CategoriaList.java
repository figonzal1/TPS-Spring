package com.ts.mscategoria.dominio;

import java.util.List;

public class CategoriaList {

	private List<CategoriaVO> categoriaVOList;

	public CategoriaList() {
	}

	public CategoriaList(List<CategoriaVO> categoriaVOList) {
		this.categoriaVOList = categoriaVOList;
	}

	public List<CategoriaVO> getCategoriaVOList() {
		return categoriaVOList;
	}

	public void setCategoriaVOList(List<CategoriaVO> categoriaVOList) {
		this.categoriaVOList = categoriaVOList;
	}
}