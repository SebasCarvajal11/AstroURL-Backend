package com.astro.url.event;

import com.astro.url.model.Url;

/**
 * Evento inmutable que se publica cada vez que se accede a una URL acortada con éxito.
 * Contiene toda la información necesaria para que otros módulos (como el de estadísticas)
 * puedan reaccionar sin necesidad de consultar la base de datos de nuevo.
 *
 * @param url        La entidad Url completa que fue accedida.
 * @param ipAddress  La dirección IP del cliente que accedió a la URL.
 * @param userAgent  El User-Agent del navegador o cliente.
 */
public record UrlAccessedEvent(Url url, String ipAddress, String userAgent) {
}