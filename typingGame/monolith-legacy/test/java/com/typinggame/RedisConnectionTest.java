package com.typinggame;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.Assert;

/**
 * Redis连接测试类，用于测试Redis是否能够正常连接和工作
 */
@SpringBootTest
class RedisConnectionTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 测试Redis连接和基本操作
     */
    @Test
    void testRedisConnection() {

        // 测试连接
        Assert.notNull(redisTemplate, "RedisTemplate should not be null");
        Assert.notNull(redisTemplate.getConnectionFactory(), "RedisConnectionFactory should not be null");

        // 测试基本操作
        String key = "test_key";
        String value = "test_value";

        // 设置键值对
        redisTemplate.opsForValue().set(key, value);

        // 获取值
        Object result = redisTemplate.opsForValue().get(key);
        Assert.notNull(result, "Result should not be null");
        Assert.isInstanceOf(String.class, result, "Result should be a String");
        Assert.isTrue(value.equals(result), "Value should match");

       // 删除键
        Boolean deleteResult = redisTemplate.delete(key);
        Assert.isTrue(deleteResult, "Delete should return true");

        // 验证键已删除
        Object deletedValue = redisTemplate.opsForValue().get(key);
        Assert.isNull(deletedValue, "Value should be null after deletion");

        System.out.println("Redis connection test passed successfully!");
    }

    /**
     * 测试Redis哈希操作
     */
    @Test
    void testRedisHashOperations() {
        String hashKey = "test_hash";

        // 设置哈希字段
        redisTemplate.opsForHash().put(hashKey, "field1", "value1");
        redisTemplate.opsForHash().put(hashKey, "field2", "value2");

        // 获取哈希字段值
        Object field1 = redisTemplate.opsForHash().get(hashKey, "field1");
        Assert.notNull(field1, "Field1 should not be null");
        Assert.isTrue("value1".equals(field1), "Field1 value should match");

        // 获取所有哈希字段
        Long size = redisTemplate.opsForHash().size(hashKey);
        Assert.isTrue(size == 2, "Hash size should be 2");

        // 删除哈希字段
        redisTemplate.opsForHash().delete(hashKey, "field1");
        Long newSize = redisTemplate.opsForHash().size(hashKey);
        Assert.isTrue(newSize == 1, "Hash size should be 1 after deletion");

        // 删除整个哈希
        redisTemplate.delete(hashKey);
        Long finalSize = redisTemplate.opsForHash().size(hashKey);
        Assert.isTrue(finalSize == 0, "Hash size should be 0 after deletion");

        System.out.println("Redis hash operations test passed successfully!");
    }

    /**
     * 测试Redis列表操作
     */
    @Test
    void testRedisListOperations() {
        String listKey = "test_list";

        // 向列表左侧添加元素
        redisTemplate.opsForList().leftPush(listKey, "element1");
        redisTemplate.opsForList().leftPush(listKey, "element2");
        redisTemplate.opsForList().leftPush(listKey, "element3");

        // 获取列表长度
        Long size = redisTemplate.opsForList().size(listKey);
        Assert.isTrue(size == 3, "List size should be 3");

        // 从列表左侧弹出元素
        Object element = redisTemplate.opsForList().leftPop(listKey);
        Assert.notNull(element, "Popped element should not be null");
        Assert.isTrue("element3".equals(element), "Popped element should be element3");
        
        // 验证列表长度
        Long newSize = redisTemplate.opsForList().size(listKey);
        Assert.isTrue(newSize == 2, "List size should be 2 after popping");
        
        // 删除整个列表
        redisTemplate.delete(listKey);
        Long finalSize = redisTemplate.opsForList().size(listKey);
        Assert.isTrue(finalSize == 0, "List size should be 0 after deletion");
        
        System.out.println("Redis list operations test passed successfully!");
    }
}