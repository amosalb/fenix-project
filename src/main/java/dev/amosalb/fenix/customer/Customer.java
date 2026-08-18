package dev.amosalb.fenix.customer;

import jakarta.persistence.*;

@Entity
    @Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "public_id")
    private String publicId;

    @Enumerated(EnumType.ORDINAL)
    private PublicIdType publicIdType;

    private String email;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public PublicIdType getPublicIdType() {
        return publicIdType;
    }

    public void setPublicIdType(PublicIdType publicIdType) {
        this.publicIdType = publicIdType;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
