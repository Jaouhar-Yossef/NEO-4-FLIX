package com.neo4flix.movieservice.common.exception;

public class MovieNotFoundException extends RuntimeException {

    public MovieNotFoundException(String id) {
        super("Movie not found with id: " + id);
    }
}
