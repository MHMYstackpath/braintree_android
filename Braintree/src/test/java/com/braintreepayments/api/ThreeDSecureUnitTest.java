package com.braintreepayments.api;

import android.content.Context;

import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.ManifestValidator;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.ThreeDSecurePostalAddress;
import com.braintreepayments.api.models.ThreeDSecureRequest;
import com.braintreepayments.testutils.TestConfigurationBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.spy;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*", "javax.crypto.*" })
@PrepareForTest({ ManifestValidator.class })
public class ThreeDSecureUnitTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private MockFragmentBuilder mMockFragmentBuilder;
    private BraintreeFragment mFragment;

    @Before
    public void setup() throws Exception {
        spy(ManifestValidator.class);
        doReturn(true).when(ManifestValidator.class, "isUrlSchemeDeclaredInAndroidManifest", any(Context.class), anyString(), any(Class.class));

        Configuration configuration = new TestConfigurationBuilder()
                .threeDSecureEnabled(true)
                .buildConfiguration();

        mMockFragmentBuilder = new MockFragmentBuilder()
                .authorization(Authorization.fromString(stringFromFixture("base_64_client_token.txt")))
                .configuration(configuration);
        mFragment = mMockFragmentBuilder.build();

        mFragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                fail(error.getMessage());
            }
        });
    }

    @Test
    public void performVerification_sendsAllParamatersInLookupRequest() throws InterruptedException, JSONException {
        ThreeDSecureRequest request = new ThreeDSecureRequest()
                .nonce("a-nonce")
                .amount("1.00")
                .shippingMethod("01")
                .mobilePhoneNumber("8101234567")
                .email("test@example.com")
                .billingAddress(new ThreeDSecurePostalAddress()
                        .firstName("Joe")
                        .lastName("Guy")
                        .streetAddress("555 Smith Street")
                        .extendedAddress("#5")
                        .locality("Oakland")
                        .region("CA")
                        .postalCode("12345")
                        .countryCodeAlpha2("US")
                        .phoneNumber("12345678"));

        ThreeDSecure.performVerification(mFragment, request);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mFragment.getHttpClient()).post(anyString(), captor.capture(), any(HttpResponseCallback.class));

        JSONObject body = new JSONObject(captor.getValue());

        assertEquals("1.00", body.getString("amount"));

        JSONObject customer = body.getJSONObject("customer");

        assertEquals("8101234567", customer.getString("mobilePhoneNumber"));
        assertEquals("test@example.com", customer.getString("email"));
        assertEquals("01", customer.getString("shippingMethod"));

        JSONObject billingAddress = customer.getJSONObject("billingAddress");

        assertEquals("Joe", billingAddress.getString("firstName"));
        assertEquals("Guy", billingAddress.getString("lastName"));
        assertEquals("555 Smith Street", billingAddress.getString("line1"));
        assertEquals("#5", billingAddress.getString("line2"));
        assertEquals("Oakland", billingAddress.getString("city"));
        assertEquals("CA", billingAddress.getString("state"));
        assertEquals("12345", billingAddress.getString("postalCode"));
        assertEquals("US", billingAddress.getString("countryCode"));
        assertEquals("12345678", billingAddress.getString("phoneNumber"));
    }

    @Test
    public void performVerification_sendsMinimumParamatersInLookupRequest() throws InterruptedException, JSONException {
        ThreeDSecureRequest request = new ThreeDSecureRequest()
                .nonce("a-nonce")
                .amount("1.00");

        ThreeDSecure.performVerification(mFragment, request);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mFragment.getHttpClient()).post(anyString(), captor.capture(), any(HttpResponseCallback.class));

        JSONObject body = new JSONObject(captor.getValue());

        assertEquals("1.00", body.getString("amount"));

        JSONObject customer = body.getJSONObject("customer");

        assertTrue(customer.isNull("mobilePhoneNumber"));
        assertTrue(customer.isNull("email"));
        assertTrue(customer.isNull("shippingMethod"));
        assertTrue(customer.isNull("billingAddress"));
    }

    @Test
    public void performVerification_sendsPartialParamatersInLookupRequest() throws InterruptedException, JSONException {
        ThreeDSecureRequest request = new ThreeDSecureRequest()
                .nonce("a-nonce")
                .amount("1.00")
                .email("test@example.com")
                .billingAddress(new ThreeDSecurePostalAddress()
                        .firstName("Joe")
                        .lastName("Guy")
                        .streetAddress("555 Smith Street")
                        .locality("Oakland")
                        .region("CA")
                        .postalCode("12345")
                        .countryCodeAlpha2("US"));

        ThreeDSecure.performVerification(mFragment, request);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mFragment.getHttpClient()).post(anyString(), captor.capture(), any(HttpResponseCallback.class));

        JSONObject body = new JSONObject(captor.getValue());

        assertEquals("1.00", body.getString("amount"));

        JSONObject customer = body.getJSONObject("customer");

        assertTrue(customer.isNull("mobilePhoneNumber"));
        assertEquals("test@example.com", customer.getString("email"));
        assertTrue(customer.isNull("shippingMethod"));

        JSONObject billingAddress = customer.getJSONObject("billingAddress");

        assertEquals("Joe", billingAddress.getString("firstName"));
        assertEquals("Guy", billingAddress.getString("lastName"));
        assertEquals("555 Smith Street", billingAddress.getString("line1"));
        assertTrue(billingAddress.isNull("line2"));
        assertEquals("Oakland", billingAddress.getString("city"));
        assertEquals("CA", billingAddress.getString("state"));
        assertEquals("12345", billingAddress.getString("postalCode"));
        assertEquals("US", billingAddress.getString("countryCode"));
        assertTrue(billingAddress.isNull("phoneNumber"));
    }
}