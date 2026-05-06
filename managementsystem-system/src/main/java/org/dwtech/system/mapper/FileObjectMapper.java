package org.dwtech.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.dwtech.system.model.entity.FileObjectPO;
/**
 * FileObjectMapper
 *
 * @author steve12311
 * @since 2026-02-26
 */

@Mapper
public interface FileObjectMapper extends BaseMapper<FileObjectPO> {

    /**
     * 绕过逻辑删除过滤器，按 sha256 + fileSize 查找文件对象（包含已删除的）。
     */
    @Select("SELECT id, sha256, file_size, storage_path, mime_type, ext, ref_count, is_deleted, create_time, update_time " +
            "FROM sys_file_object WHERE sha256 = #{sha256} AND file_size = #{fileSize} LIMIT 1")
    FileObjectPO selectByHashIgnoreDeleted(@Param("sha256") String sha256, @Param("fileSize") long fileSize);

    /**
     * 激活已被逻辑删除的文件对象：恢复 is_deleted=0、ref_count=1、更新存储路径。
     */
    @Update("UPDATE sys_file_object SET is_deleted = 0, ref_count = 1, storage_path = #{storagePath} WHERE id = #{id}")
    int reactivateFileObject(@Param("id") Long id, @Param("storagePath") String storagePath);
}
