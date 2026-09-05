package com.xxx.cases.entity

@jakarta.persistence.Entity @jakarta.persistence.Table public final data class CaseBE public constructor(id: com.bmw.cc.csvbe.bm.cases.CASE_ID /* = java.util.UUID */ = COMPILED_CODE, createdAt: java.time.OffsetDateTime, updatedAt: java.time.OffsetDateTime, breakdownCity: kotlin.String) : com.bmw.cc.csvbe.persistence.AbstractAuditedBaseBE {
    @field:jakarta.persistence.Id public open val id: com.bmw.cc.csvbe.bm.cases.CASE_ID /* = java.util.UUID */ /* compiled code */

    public open var createdAt: java.time.OffsetDateTime /* compiled code */

    public open var updatedAt: java.time.OffsetDateTime /* compiled code */

    public final var breakdownFormattedAddress: kotlin.String /* compiled code */

    public final operator fun component1(): com.bmw.cc.csvbe.bm.cases.CASE_ID /* = java.util.UUID */ { /* compiled code */ }

    public final operator fun component2(): java.time.OffsetDateTime { /* compiled code */ }

    public final operator fun component3(): java.time.OffsetDateTime { /* compiled code */ }

    public final operator fun component4(): kotlin.String { /* compiled code */ }

    public open operator fun equals(other: kotlin.Any?): kotlin.Boolean { /* compiled code */ }

    public open fun hashCode(): kotlin.Int { /* compiled code */ }

    public open fun toString(): kotlin.String { /* compiled code */ }
}
