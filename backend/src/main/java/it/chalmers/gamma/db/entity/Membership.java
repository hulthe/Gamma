package it.chalmers.gamma.db.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.chalmers.gamma.db.entity.pk.MembershipPK;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "membership")
public class Membership {

    @EmbeddedId
    @JsonIgnore
    private MembershipPK id;

    @JoinColumn(name = "ituser_id")
    @MapsId("ituserId")
    @ManyToOne
    private ITUser itUser;

    @JoinColumn(name = "fkit_group_id")
    @MapsId("fkit_group")
    @ManyToOne
    private FKITGroup fkitGroup;

    @ManyToOne
    private Post post;

    @Column(name = "unofficial_post_name", length = 100)
    private String unofficialPostName;

    public MembershipPK getId() {
        return id;
    }

    public void setId(MembershipPK id) {
        this.id = id;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public String getUnofficialPostName() {
        return unofficialPostName;
    }

    public void setUnofficialPostName(String unofficialPostName) {
        this.unofficialPostName = unofficialPostName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Membership that = (Membership) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(post, that.post) &&
                Objects.equals(unofficialPostName, that.unofficialPostName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, post, unofficialPostName);
    }

    @Override
    public String toString() {
        return "Membership{" +
                "id=" + id +
                ", post=" + post +
                ", unofficialPostName='" + unofficialPostName + '\'' +
                '}';
    }
}
