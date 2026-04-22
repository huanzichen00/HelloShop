package com.huanzichen.springboothello.mapper;

import com.huanzichen.springboothello.model.UserInfo;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    @Insert("insert into users(name, age) values (#{name}, #{age})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserInfo userInfo);

    @Select("select id, name, age from users")
    List<UserInfo> findAll();

    @Select("select id, name, age, username, password from users where id = #{id}")
    UserInfo findById(Long id);

    @Delete("delete from users where id = #{id}")
    int deleteById(Long id);

    @Update("update users set name = #{name}, age = #{age} where id = #{id}")
    int update(UserInfo userInfo);

    @Select("select id, name, age from users where name like concat('%', #{name}, '%')")
    List<UserInfo> searchByName(String name);

    @Select("select id, name, age from users limit #{offset}, #{size}")
    List<UserInfo> findPage(Integer offset, Integer size);

    @Select("select count(*) from users")
    Long countAll();

    @Select("select id, name, age from users where name like concat('%', #{name}, '%') limit #{offset}, #{size}")
    List<UserInfo> searchByNameWithPage(String name, Integer offset, Integer size);

    List<UserInfo> findPageWithSort(@Param("offset") Integer offset,
                                    @Param("size") Integer size,
                                    @Param("sort") String sort,
                                    @Param("order") String order);

    List<UserInfo> searchByNameWithPageAndSort(@Param("name") String name,
                                               @Param("offset") Integer offset,
                                               @Param("size") Integer size,
                                               @Param("sort") String sort,
                                               @Param("order") String order);

    Long countByName(@Param("name") String name);

    List<UserInfo> searchWithConditionsAndPage(@Param("name") String name,
                                               @Param("minAge") Integer minAge,
                                               @Param("maxAge") Integer maxAge,
                                               @Param("offset") Integer offset,
                                               @Param("size") Integer size,
                                               @Param("sort") String sort,
                                               @Param("order") String order);

    Long countWithConditions(@Param("name") String name,
                             @Param("minAge") Integer minAge,
                             @Param("maxAge") Integer maxAge);

    @Insert("insert into users(username, password, name, age) values (#{username}, #{password}, #{name}, #{age})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertWithAuth(UserInfo userInfo);

    @Select("select id, username, password, name, age from users where username = #{username}")
    UserInfo findByUsername(String username);
}
