package com.eventpro.entity;

import com.eventpro.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "agents_controle")
@Getter @Setter @NoArgsConstructor
public class AgentControle extends User {

    @OneToMany(mappedBy = "agent")
    private List<ControleBillet> controles = new ArrayList<>();
}
