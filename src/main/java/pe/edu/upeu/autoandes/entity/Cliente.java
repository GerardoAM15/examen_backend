package pe.edu.upeu.autoandes.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "cliente")
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 11)
    private String documento;
    @Column(nullable = false, length = 120)
    private String nombres;
    @Column(length = 15)
    private String telefono;
}

