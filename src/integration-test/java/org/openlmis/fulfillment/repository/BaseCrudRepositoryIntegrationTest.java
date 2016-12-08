package org.openlmis.fulfillment.repository;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openlmis.fulfillment.domain.BaseEntity;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Transactional
@SpringBootTest
@DirtiesContext
@RunWith(SpringRunner.class)
public abstract class BaseCrudRepositoryIntegrationTest<T extends BaseEntity> {

  private AtomicInteger instanceNumber = new AtomicInteger(0);

  /**
   * Retrieve an instance of repository for entity type.
   *
   * @return repository
   */
  abstract CrudRepository<T, UUID> getRepository();

  /**
   * Generate a unique instance of given type.
   *
   * @return generated instance
   */
  abstract T generateInstance();

  int getNextInstanceNumber() {
    return this.instanceNumber.incrementAndGet();
  }

  void assertInstance(T instance) {
    Assert.assertNotNull(instance.getId());
  }

  @Test
  public void testCreate() {
    CrudRepository<T, UUID> repository = this.getRepository();

    T instance = this.generateInstance();
    Assert.assertNull(instance.getId());

    instance = repository.save(instance);
    assertInstance(instance);

    Assert.assertTrue(repository.exists(instance.getId()));
  }

  @Test
  public void testFindOne() {
    CrudRepository<T, UUID> repository = this.getRepository();

    T instance = this.generateInstance();

    instance = repository.save(instance);
    assertInstance(instance);

    UUID id = instance.getId();

    instance = repository.findOne(id);
    assertInstance(instance);
    Assert.assertEquals(id, instance.getId());
  }

  @Test
  public void testDelete() {
    CrudRepository<T, UUID> repository = this.getRepository();

    T instance = this.generateInstance();
    Assert.assertNotNull(instance);

    instance = repository.save(instance);
    assertInstance(instance);

    UUID id = instance.getId();

    repository.delete(id);
    Assert.assertFalse(repository.exists(id));
  }
}
