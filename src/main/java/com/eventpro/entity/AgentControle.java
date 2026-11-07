package com.eventpro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "agents_controle")
@Getter @Setter @NoArgsConstructor
public class AgentControle extends User {
}