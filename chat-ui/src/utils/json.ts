/**
 * JSON 工具 — 解决 JavaScript 的 Long 精度丢失问题。
 *
 * 后端 Snowflake 算法生成的 ID 为 64 位 Long 类型，
 * JavaScript Number 只能安全表示 ±2^53 以内的整数，
 * 超过此范围的数字会被四舍五入，导致精度丢失。
 *
 * 本工具使用 json-bigint 库，将 JSON 中的所有数字解析为字符串，
 * 确保 ID 等大整数在传输过程中不丢失精度。
 *
 * 同时提供安全序列化方法，自动保护 ID 字段不被 Number 截断。
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
 * ID 字段名模式：字段名以 "Id" 或 "id" 结尾，或就是 "id"
 */
const ID_KEY_PATTERN = /^(id|[a-zA-Z0-9]+[Ii][Dd])$/

function isIdKey(key: string): boolean {
  return ID_KEY_PATTERN.test(key) || key.endsWith('Id') || key.endsWith('_id')
}

/**
 * 安全序列化 JSON，自动将 ID 字段中的数字转为字符串，防止精度丢失。
 *
 * 使用场景：WebSocket.send() 时替代 JSON.stringify
 *
 * @example
 * const msg = { messageId: 123456789012345678, content: 'hi' }
 * safeStringify(msg) // → {"messageId":"123456789012345678","content":"hi"}
 */
export function safeStringify(value: unknown): string {
  return JSON.stringify(value, (key, val) => {
    if (val !== null && typeof val === 'number' && isIdKey(key)) {
      return String(val)
    }
    return val
  })
}

/**
 * 标准序列化（原生 JSON.stringify，用于不含大整数的普通对象）。
 */
export const nativeJson = JSON
