package com.eventpro.entity;

import com.eventpro.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "administrateurs")
@Getter @Setter @NoArgsConstructor
public class Administrateur extends User {

}
