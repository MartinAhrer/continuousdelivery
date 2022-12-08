package at.martinahrer.cd.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.AbstractPersistable;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Getter
@Setter
public class Company extends AbstractPersistable<Long> {

    @NotNull
    @Size(min=3, max = 255)
    private String name;

    @OneToOne(cascade = CascadeType.ALL)
    private Address address;
}
