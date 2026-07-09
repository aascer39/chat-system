package com.zjj.chatsystem.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 分页返回封装
 */
public class PageResult<T> {

    private int code;
    private String message;
    private List<T> records;
    private long total;
    private long page;
    private long pageSize;
    private long timestamp;

    private PageResult() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> PageResult<T> success(IPage<T> page) {
        PageResult<T> r = new PageResult<>();
        r.code = 200;
        r.message = "success";
        r.records = page.getRecords();
        r.total = page.getTotal();
        r.page = page.getCurrent();
        r.pageSize = page.getSize();
        return r;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public List<T> getRecords() {
        return records;
    }

    public long getTotal() {
        return total;
    }

    public long getPage() {
        return page;
    }

    public long getPageSize() {
        return pageSize;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
