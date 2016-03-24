package at.martinahrer.cd.model;


import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Entity;

@Entity
@Getter
@Setter
public class Address extends AbstractPersistable<Long> {
    private String line1;
    private String line2;
    private String zip;
    private String city;
    private String state;
    private String country;
}
