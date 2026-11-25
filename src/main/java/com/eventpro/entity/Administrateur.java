package com.eventpro.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "administrateurs")
@Getter @Setter @NoArgsConstructor
public class Administrateur extends User {

}
