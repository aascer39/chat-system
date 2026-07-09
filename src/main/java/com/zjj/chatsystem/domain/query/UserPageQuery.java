package com.zjj.chatsystem.domain.query;

/**
 * 用户分页查询参数
 */
public class UserPageQuery {

    private Integer page = 1;

    private Integer pageSize = 10;

    private String keyword;

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
