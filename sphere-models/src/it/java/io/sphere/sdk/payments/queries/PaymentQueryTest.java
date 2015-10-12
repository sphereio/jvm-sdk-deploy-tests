package io.sphere.sdk.payments.queries;

import io.sphere.sdk.customers.CustomerFixtures;
import io.sphere.sdk.payments.*;
import io.sphere.sdk.payments.commands.PaymentCreateCommand;
import io.sphere.sdk.payments.commands.PaymentDeleteCommand;
import io.sphere.sdk.test.IntegrationTest;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.TypeFixtures;
import org.junit.Test;

import javax.money.MonetaryAmount;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static io.sphere.sdk.states.StateFixtures.withStateByBuilder;
import static io.sphere.sdk.states.StateType.PAYMENT_STATE;
import static io.sphere.sdk.test.SphereTestUtils.*;
import static io.sphere.sdk.test.SphereTestUtils.EURO_1;
import static io.sphere.sdk.test.SphereTestUtils.asList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.*;

public class PaymentQueryTest extends IntegrationTest {
    @Test
    public void fullTest() {
        withStateByBuilder(client(), stateBuilder -> stateBuilder.initial(true).type(PAYMENT_STATE), paidState -> {
            TypeFixtures.withUpdateableType(client(), type -> {
                CustomerFixtures.withCustomerAndCart(client(), ((customer, cart) -> {
                    final MonetaryAmount totalAmount = cart.getTotalPrice();
                    final PaymentStatus paymentStatus = PaymentStatusBuilder.of().interfaceCode(randomKey()).interfaceText(randomString()).state(paidState).build();
                    final PaymentMethodInfo paymentMethodInfo = PaymentMethodInfoBuilder.of()
                            .paymentInterface("STRIPE")
                            .method("CREDIT_CARD")
                            .name(randomSlug())
                            .build();
                    final List<Transaction> transactions = Collections.singletonList(TransactionBuilder
                            .of(TransactionType.CHARGE, totalAmount, ZonedDateTime.now())
                            .timestamp(ZonedDateTime.now())
                            .interactionId(randomKey())
                            .build());
                    final String externalId = randomKey();
                    final String interfaceId = randomKey();
                    final ZonedDateTime authorizedUntil = ZonedDateTime.now().plusMonths(1);
                    final PaymentDraftBuilder paymentDraftBuilder = PaymentDraftBuilder.of(totalAmount)
                            .customer(customer)
                            .externalId(externalId)
                            .interfaceId(interfaceId)
                            .amountAuthorized(totalAmount)
                            .amountPaid(totalAmount)
                            .authorizedUntil(authorizedUntil)
                            .amountRefunded(EURO_1)
                            .paymentMethodInfo(paymentMethodInfo)
                            .custom(CustomFieldsDraft.ofTypeKeyAndObjects(type.getKey(), singletonMap(TypeFixtures.STRING_FIELD_NAME, "foo")))
                            .paymentStatus(paymentStatus)
                            .transactions(transactions)
                            .interfaceInteractions(asList("foo1", "foo2").stream()
                                    .map(s -> CustomFieldsDraft.ofTypeKeyAndObjects(type.getKey(), singletonMap(TypeFixtures.STRING_FIELD_NAME, s)))
                                    .collect(toList()));
                    final Payment payment = execute(PaymentCreateCommand.of(paymentDraftBuilder.build()));

                    final PaymentQuery paymentQuery = PaymentQuery.of()
                            .withPredicates(m -> m.id().is(payment.getId())
                                            .and(m.customer().is(customer))
                                            .and(m.externalId().is(externalId))
                                            .and(m.interfaceId().is(interfaceId))
                                            .and(m.amountPlanned().currencyCode().is(totalAmount.getCurrency()))
                                            .and(m.amountAuthorized().currencyCode().is(totalAmount.getCurrency()))
                                            .and(m.amountPaid().currencyCode().is(totalAmount.getCurrency()))
                                            .and(m.amountRefunded().currencyCode().is(totalAmount.getCurrency()))
                                            .and(m.paymentMethodInfo().method().is(paymentMethodInfo.getMethod()))
                                            .and(m.paymentMethodInfo().paymentInterface().is(paymentMethodInfo.getPaymentInterface()))
                                            .and(m.paymentMethodInfo().name().locale(ENGLISH).is(paymentMethodInfo.getName().get(ENGLISH)))
                                            .and(m.custom().type().is(type))
//                                            .and(m.paymentStatus().interfaceCode().is(paymentStatus.getInterfaceCode()))
//                                            .and(m.paymentStatus().interfaceText().is(paymentStatus.getInterfaceText()))
                                            .and(m.paymentStatus().state().is(paidState))
                                    .and(m.transactions().amount().currencyCode().is(totalAmount.getCurrency()))
                                    .and(m.transactions().interactionId().is(transactions.get(0).getInteractionId()))


                            );

                    assertThat(execute(paymentQuery)).has(onlyTheResult(payment));

                    execute(PaymentDeleteCommand.of(payment));
                }));
                return type;
            });
        });
    }

}