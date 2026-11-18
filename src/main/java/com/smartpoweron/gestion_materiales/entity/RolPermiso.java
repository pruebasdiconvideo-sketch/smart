package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rol_permiso")
@IdClass(RolPermiso.RolPermisoId.class)
@Getter @Setter @NoArgsConstructor
public class RolPermiso {
    @Id
    @Column(name = "id_rol")
    private Long idRol;

    @Id
    @Column(name = "id_permiso")
    private Long idPermiso;

    public RolPermiso(Long idRol, Long idPermiso) {
        this.idRol = idRol; this.idPermiso = idPermiso;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class RolPermisoId implements java.io.Serializable {
        private Long idRol;
        private Long idPermiso;
    }
}
