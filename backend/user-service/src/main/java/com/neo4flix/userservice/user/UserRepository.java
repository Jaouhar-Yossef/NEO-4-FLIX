package com.neo4flix.userservice.user;

import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

public interface UserRepository extends Neo4jRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Deletes the user and every relationship touching it (RATED, WATCHLIST, FRIEND, ...). */
    @Query("MATCH (u:User {id: $id}) DETACH DELETE u")
    void deleteWithRelationships(String id);
}
