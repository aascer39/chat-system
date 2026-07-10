/**
 * JSON 解析工具 — 解决 JavaScript 的 Long 精度丢失问题。
 *
 * 后端 Snowflake 算法生成的 ID 为 64 位 Long 类型，
 * JavaScript Number 只能安全表示 ±2^53 以内的整数，
 * 超过此范围的数字会被四舍五入，导致精度丢失。
 *
 * 本工具使用 json-bigint 库，将 JSON 中的所有数字解析为字符串，
 * 确保 ID 等大整数在传输过程中不丢失精度。
 */

import JSONBigInt from 'json-bigint'

const jsonParser = JSONBigInt({ storeAsString: true })

/**
 * 安全解析 JSON 字符串，大整数以字符串形式保留。
 */
export function safeParse(text: string): unknown {
  return jsonParser.parse(text)
}

/**
 * 标准序列化（仍使用原生 JSON.stringify，兼容性更好）。
 * 不含 BigInt 对象的普通对象可直接使用。
 */
export const nativeJson = JSON
