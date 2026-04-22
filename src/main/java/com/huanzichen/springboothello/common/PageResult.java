package com.huanzichen.springboothello.common;

import java.util.List;

public class PageResult<T> {
    private Long total;
    private List<T> list;
    private Integer page;
    private Integer size;
    private Integer totalPages;

    public PageResult(Long total, List<T> list, Integer page, Integer size, Integer totalPages) {
        this.total = total;
        this.list = list;
        this.page = page;
        this.size = size;
        this.totalPages = totalPages;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }
}
