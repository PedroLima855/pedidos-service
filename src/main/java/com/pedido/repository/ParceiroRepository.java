package com.pedido.repository;

import com.pedido.entity.Parceiro;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ParceiroRepository extends JpaRepository<Parceiro, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Parceiro p WHERE p.id = :id")
    Optional<Parceiro> findByIdComLock(Long id);
}
