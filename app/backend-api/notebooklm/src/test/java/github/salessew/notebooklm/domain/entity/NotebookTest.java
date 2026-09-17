package github.salessew.notebooklm.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotebookTest {

    @Test
    void deve_instanciar_e_acessar_atributos_corretamente() {
        UUID id = UUID.randomUUID();
        User owner = new User("sub-1", "owner@example.com", "Owner");
        Instant now = Instant.now();

        Notebook notebook = new Notebook(owner, "My Notebook", "Description");
        notebook.setId(id);
        notebook.setCreatedAt(now);
        notebook.setUpdatedAt(now);

        assertThat(notebook.getId()).isEqualTo(id);
        assertThat(notebook.getOwner()).isEqualTo(owner);
        assertThat(notebook.getName()).isEqualTo("My Notebook");
        assertThat(notebook.getDescription()).isEqualTo("Description");
        assertThat(notebook.getCreatedAt()).isEqualTo(now);
        assertThat(notebook.getUpdatedAt()).isEqualTo(now);

        User newOwner = new User("sub-2", "owner2@example.com", "Owner 2");
        notebook.setOwner(newOwner);
        notebook.setName("New Name");
        notebook.setDescription("New Desc");

        assertThat(notebook.getOwner()).isEqualTo(newOwner);
        assertThat(notebook.getName()).isEqualTo("New Name");
        assertThat(notebook.getDescription()).isEqualTo("New Desc");
    }

    @Test
    void deve_testar_equals_e_hashcode_baseado_em_id() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        Notebook n1 = new Notebook();
        n1.setId(id1);

        Notebook n2 = new Notebook();
        n2.setId(id1);

        Notebook n3 = new Notebook();
        n3.setId(id2);

        assertThat(n1).isEqualTo(n1);
        assertThat(n1).isEqualTo(n2);
        assertThat(n1).hasSameHashCodeAs(n2);

        assertThat(n1).isNotEqualTo(n3);
        assertThat(n1).isNotEqualTo(null);
        assertThat(n1).isNotEqualTo("another object");

        Notebook empty1 = new Notebook();
        Notebook empty2 = new Notebook();
        assertThat(empty1).isNotEqualTo(empty2);

        assertThat(n1.hashCode()).isNotZero();
    }
}
