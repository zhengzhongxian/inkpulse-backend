package com.inkpulse.entities;

import com.inkpulse.generator.GeneratedUuidV7;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity<TKey extends Serializable> {

    @Id
    @GeneratedUuidV7
    private TKey id;
}
