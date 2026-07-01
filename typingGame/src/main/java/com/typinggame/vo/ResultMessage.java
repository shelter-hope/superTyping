package com.typinggame.vo;

import lombok.Data;
import java.io.Serializable;

/**
 * 统一响应结构
 */
@Data
public class ResultMessage<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 消息
     */
    private String msg;

    /**
     * 数据
     */
    private T data;

    public static <T> ResultMessage<T> success(T data) {
        ResultMessage<T> r = new ResultMessage<>();
        r.setCode(200);
        r.setMsg("success");
        r.setData(data);
        return r;
    }

    public static <T> ResultMessage<T> success() {
        return success(null);
    }

    public static <T> ResultMessage<T> error(Integer code, String msg) {
        ResultMessage<T> r = new ResultMessage<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }

    public static <T> ResultMessage<T> error(String msg) {
        return error(500, msg);
    }
}
