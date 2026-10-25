package com.eventpro.repository;

import com.eventpro.entity.Administrateur;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministrateurRepository extends JpaRepository<Administrateur, Long> {
}
