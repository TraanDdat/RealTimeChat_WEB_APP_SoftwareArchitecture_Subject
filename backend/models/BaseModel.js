// models/BaseModel.js
import pool from '../config/db.js';

class BaseModel {
  constructor(tableName) {
    this.tableName = tableName;
  }

  // CREATE: Tự động map keys và values để INSERT
  async create(data) {
    const keys = Object.keys(data);
    const values = Object.values(data);
    
    // Tạo mảng placeholders: $1, $2, $3...
    const placeholders = keys.map((_, i) => `$${i + 1}`).join(", ");
    const columns = keys.join(", ");

    const query = `INSERT INTO ${this.tableName} (${columns}) VALUES (${placeholders}) RETURNING *`;
    
    const { rows } = await pool.query(query, values);
    return rows[0];
  }

  // FIND ONE: Tìm kiếm một bản ghi dựa trên điều kiện linh hoạt (id, email, v.v.)
  async findOne(conditions) {
    const keys = Object.keys(conditions);
    const values = Object.values(conditions);
    
    // Xây dựng chuỗi WHERE: "id = $1 AND email = $2"
    const whereClause = keys.map((key, i) => `${key} = $${i + 1}`).join(" AND ");

    const query = `SELECT * FROM ${this.tableName} WHERE ${whereClause} LIMIT 1`;
    
    const { rows } = await pool.query(query, values);
    return rows[0];
  }

  // UPDATE: Cập nhật dữ liệu dựa trên điều kiện
  async update(conditions, dataToUpdate) {
    const dataKeys = Object.keys(dataToUpdate);
    const dataValues = Object.values(dataToUpdate);
    
    if (dataKeys.length === 0) return null;

    // Chuỗi SET: "name = $1, password = $2"
    const setClause = dataKeys.map((key, i) => `${key} = $${i + 1}`).join(", ");

    const condKeys = Object.keys(conditions);
    const condValues = Object.values(conditions);
    
    // Offset index cho điều kiện WHERE bắt đầu từ sau các index của SET
    const offset = dataKeys.length;
    const whereClause = condKeys.map((key, i) => `${key} = $${offset + i + 1}`).join(" AND ");

    const query = `UPDATE ${this.tableName} SET ${setClause} WHERE ${whereClause} RETURNING *`;
    
    // Gộp mảng giá trị của SET và WHERE lại để query
    const { rows } = await pool.query(query, [...dataValues, ...condValues]);
    return rows[0];
  }

  // REMOVE: Xóa bản ghi dựa trên điều kiện
  async remove(conditions) {
    const keys = Object.keys(conditions);
    const values = Object.values(conditions);
    
    const whereClause = keys.map((key, i) => `${key} = $${i + 1}`).join(" AND ");

    const query = `DELETE FROM ${this.tableName} WHERE ${whereClause} RETURNING *`;
    
    const { rows } = await pool.query(query, values);
    return rows[0];
  }
}

export default BaseModel;