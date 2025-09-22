package br.distributed.system.chat.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "group")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

}
