package cn.enilu.flash.bean.entity.shop;

import cn.enilu.flash.bean.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.hibernate.annotations.Table;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

/**
 */

@Data
@Table(appliesTo = "t_shop_attr_val",comment = "商品属性值")
@Entity(name="t_shop_attr_val")
@EntityListeners(AuditingEntityListener.class)
public class AttrVal extends BaseEntity {
    @Column(name="id_attr_key",columnDefinition = "BIGINT COMMENT '属性id'")
    private Long idAttrKey;
    @Column(name="attr_val",columnDefinition = "VARCHAR(32) COMMENT '属性值'")
    private String attrVal;
    @JoinColumn(name = "id_attr_key", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private AttrKey attrKey;

}
