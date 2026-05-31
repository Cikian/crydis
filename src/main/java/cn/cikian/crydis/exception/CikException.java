package cn.cikian.crydis.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * Cik异常
 *
 * @author Cikian
 * @version 1.0
 * @implNote
 * @see <a href="https://www.cikian.cn">https://www.cikian.cn</a>
 * @since 2025-03-20 20:35
 */
@Getter
@Setter
public class CikException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private Integer code;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 构造方法
     *
     * @param message 错误消息
     */
    public CikException(String message) {
        super(message);
        this.message = message;
        this.code = 500;
    }

    /**
     * 构造方法
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public CikException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造方法
     *
     * @param code    错误码
     * @param message 错误消息
     * @param cause   异常原因
     */
    public CikException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造方法
     *
     * @param message 错误消息
     * @param cause   异常原因
     */
    public CikException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
        this.code = 500;
    }
}
