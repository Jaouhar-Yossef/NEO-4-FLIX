package com.neo4flix.movieservice.genre;

import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface GenreRepository extends Neo4jRepository<Genre, String> {

    Optional<Genre> findByName(String name);
}
