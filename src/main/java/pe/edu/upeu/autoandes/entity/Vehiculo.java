package pe.edu.upeu.autoandes.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "vehiculo")
public class Vehiculo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 7)
    private String placa;
    @Column(nullable = false, length = 40)
    private String marca;
    @Column(nullable = false, length = 40)
    private String modelo;
    @Column(nullable = false)
    private Integer anio;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;
}

