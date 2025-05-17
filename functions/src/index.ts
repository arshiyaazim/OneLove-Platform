import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import * as express from 'express';
import * as cors from 'cors';
import Stripe from 'stripe';

// Initialize Firebase Admin
admin.initializeApp();

// Initialize Firestore
const db = admin.firestore();
const messaging = admin.messaging();

// Initialize Stripe
const stripeSecretKey = functions.config().stripe?.secret_key || '';
const stripe = new Stripe(stripeSecretKey, {
  apiVersion: '2023-10-16',
});

// Webhook signing secret
const webhookSecret = functions.config().stripe?.webhook_secret || '';

/**
 * Express app for handling webhook requests
 */
const app = express();
app.use(express.json({
  verify: (req, res, buf) => {
    // Save raw body for webhook verification
    (req as any).rawBody = buf.toString();
  }
}));
app.use(cors({ origin: true }));

/**
 * Create a payment intent for a purchase
 */
export const createPaymentIntent = functions.https.onCall(async (data, context) => {
  // Verify authentication
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'User must be logged in to create a payment intent'
    );
  }

  try {
    const { amount, currency = 'usd', payment_type, user_id } = data;

    if (!amount || !payment_type) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Required parameters missing'
      );
    }

    // Verify that user_id matches authenticated user
    if (user_id !== context.auth.uid) {
      throw new functions.https.HttpsError(
        'permission-denied',
        'User ID does not match authenticated user'
      );
    }

    // Create payment intent in Stripe
    const paymentIntent = await stripe.paymentIntents.create({
      amount,
      currency,
      metadata: {
        payment_type,
        user_id: context.auth.uid,
        subscription_id: data.subscription_id || '',
        offer_id: data.offer_id || ''
      }
    });

    // Return client secret and payment intent ID
    return {
      clientSecret: paymentIntent.client_secret,
      id: paymentIntent.id,
      amount: paymentIntent.amount,
      requires_action: paymentIntent.status === 'requires_action',
      payment_method_types: paymentIntent.payment_method_types,
      currency: paymentIntent.currency,
      status: paymentIntent.status,
      publishable_key: functions.config().stripe?.publishable_key || ''
    };
  } catch (error) {
    console.error('Error creating payment intent:', error);
    throw new functions.https.HttpsError(
      'internal',
      'Failed to create payment intent'
    );
  }
});

/**
 * Confirm a payment intent with a payment method
 */
export const confirmPaymentIntent = functions.https.onCall(async (data, context) => {
  // Verify authentication
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'User must be logged in to confirm a payment intent'
    );
  }

  try {
    const { payment_intent_id, payment_method_id } = data;

    if (!payment_intent_id || !payment_method_id) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Required parameters missing'
      );
    }

    // Retrieve payment intent to verify ownership
    const paymentIntent = await stripe.paymentIntents.retrieve(payment_intent_id);

    // Verify that payment intent belongs to the authenticated user
    if (paymentIntent.metadata.user_id !== context.auth.uid) {
      throw new functions.https.HttpsError(
        'permission-denied',
        'Payment intent does not belong to authenticated user'
      );
    }

    // Confirm payment intent with payment method
    const confirmedIntent = await stripe.paymentIntents.confirm(payment_intent_id, {
      payment_method: payment_method_id
    });

    return {
      id: confirmedIntent.id,
      status: confirmedIntent.status,
      next_action: confirmedIntent.next_action,
      receipt_url: confirmedIntent.charges.data[0]?.receipt_url
    };
  } catch (error) {
    console.error('Error confirming payment intent:', error);
    throw new functions.https.HttpsError(
      'internal',
      'Failed to confirm payment intent'
    );
  }
});

/**
 * Handle additional action required for payment
 */
export const handlePaymentAction = functions.https.onCall(async (data, context) => {
  // Verify authentication
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'User must be logged in to handle payment action'
    );
  }

  try {
    const { payment_id, action_result } = data;

    if (!payment_id) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Payment ID is required'
      );
    }

    // Retrieve payment intent
    const paymentIntent = await stripe.paymentIntents.retrieve(payment_id);

    // Verify that payment intent belongs to the authenticated user
    if (paymentIntent.metadata.user_id !== context.auth.uid) {
      throw new functions.https.HttpsError(
        'permission-denied',
        'Payment intent does not belong to authenticated user'
      );
    }

    // Check if payment needs further action
    if (paymentIntent.status === 'requires_action') {
      // This would typically happen client-side, but we provide server-side logic
      return {
        status: paymentIntent.status,
        next_action: paymentIntent.next_action
      };
    } else if (paymentIntent.status === 'succeeded') {
      // Payment already succeeded
      return {
        status: paymentIntent.status,
        receipt_url: paymentIntent.charges.data[0]?.receipt_url
      };
    } else {
      // Payment in other state
      return {
        status: paymentIntent.status
      };
    }
  } catch (error) {
    console.error('Error handling payment action:', error);
    throw new functions.https.HttpsError(
      'internal',
      'Failed to handle payment action'
    );
  }
});

/**
 * Create a subscription in Stripe
 */
export const createSubscription = functions.https.onCall(async (data, context) => {
  // Verify authentication
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'User must be logged in to create a subscription'
    );
  }

  try {
    const { payment_method_id, subscription_type, price_usd, auto_renew, user_id, plan_id } = data;

    if (!payment_method_id || !subscription_type || !price_usd || !plan_id) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Required parameters missing'
      );
    }

    // Verify that user_id matches authenticated user
    if (user_id !== context.auth.uid) {
      throw new functions.https.HttpsError(
        'permission-denied',
        'User ID does not match authenticated user'
      );
    }

    // Get or create customer
    let customerId = '';
    const customerSnapshot = await db.collection('stripe_customers')
      .where('userId', '==', context.auth.uid)
      .limit(1)
      .get();

    if (customerSnapshot.empty) {
      // Get user information
      const userDoc = await db.collection('users').doc(context.auth.uid).get();
      const userData = userDoc.data() || {};

      // Create new customer
      const customer = await stripe.customers.create({
        email: userData.email || context.auth.token.email || '',
        name: userData.displayName || '',
        metadata: {
          user_id: context.auth.uid
        }
      });

      customerId = customer.id;

      // Save customer to Firestore
      await db.collection('stripe_customers').doc(customer.id).set({
        userId: context.auth.uid,
        customerId: customer.id,
        email: customer.email,
        name: customer.name,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      });
    } else {
      customerId = customerSnapshot.docs[0].id;
    }

    // Attach payment method to customer
    await stripe.paymentMethods.attach(payment_method_id, {
      customer: customerId
    });

    // Set as default payment method
    await stripe.customers.update(customerId, {
      invoice_settings: {
        default_payment_method: payment_method_id
      }
    });

    // Create subscription
    const subscription = await stripe.subscriptions.create({
      customer: customerId,
      items: [
        {
          price_data: {
            currency: 'usd',
            product: plan_id,
            unit_amount: Math.round(price_usd * 100),
            recurring: {
              interval: 'month'
            }
          }
        }
      ],
      payment_behavior: 'default_incomplete',
      payment_settings: {
        payment_method_types: ['card'],
        save_default_payment_method: 'on_subscription'
      },
      expand: ['latest_invoice.payment_intent'],
      metadata: {
        user_id: context.auth.uid,
        subscription_type
      },
      cancel_at_period_end: !auto_renew
    });

    // Return subscription data
    return {
      id: subscription.id,
      status: subscription.status,
      current_period_end: subscription.current_period_end,
      cancel_at_period_end: subscription.cancel_at_period_end
    };
  } catch (error) {
    console.error('Error creating subscription:', error);
    throw new functions.https.HttpsError(
      'internal',
      'Failed to create subscription'
    );
  }
});

/**
 * Update subscription (auto-renew settings)
 */
export const updateSubscription = functions.https.onCall(async (data, context) => {
  // Verify authentication
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'User must be logged in to update a subscription'
    );
  }

  try {
    const { subscription_id, cancel_at_period_end } = data;

    if (!subscription_id) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Subscription ID is required'
      );
    }

    // Retrieve subscription to verify ownership
    const subscription = await stripe.subscriptions.retrieve(subscription_id);

    // Get customer for the subscription
    const customer = await stripe.customers.retrieve(subscription.customer as string);

    // Verify subscription belongs to user
    if (customer.metadata.user_id !== context.auth.uid) {
      throw new functions.https.HttpsError(
        'permission-denied',
        'Subscription does not belong to authenticated user'
      );
    }

    // Update subscription
    const updatedSubscription = await stripe.subscriptions.update(subscription_id, {
      cancel_at_period_end: cancel_at_period_end
    });

    return {
      id: updatedSubscription.id,
      status: updatedSubscription.status,
      cancel_at_period_end: updatedSubscription.cancel_at_period_end,
      current_period_end: updatedSubscription.current_period_end
    };
  } catch (error) {
    console.error('Error updating subscription:', error);
    throw new functions.https.HttpsError(
      'internal',
      'Failed to update subscription'
    );
  }
});

/**
 * Cancel a subscription
 */
export const cancelSubscription = functions.https.onCall(async (data, context) => {
  // Verify authentication
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'User must be logged in to cancel a subscription'
    );
  }

  try {
    const { subscription_id, cancel_immediately } = data;

    if (!subscription_id) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Subscription ID is required'
      );
    }

    // Retrieve subscription to verify ownership
    const subscription = await stripe.subscriptions.retrieve(subscription_id);
    
    // Get customer for the subscription
    const customer = await stripe.customers.retrieve(subscription.customer as string);

    // Verify subscription belongs to user
    if (customer.metadata.user_id !== context.auth.uid) {
      throw new functions.https.HttpsError(
        'permission-denied',
        'Subscription does not belong to authenticated user'
      );
    }

    let result;
    if (cancel_immediately) {
      // Cancel immediately
      result = await stripe.subscriptions.cancel(subscription_id);
    } else {
      // Cancel at period end
      result = await stripe.subscriptions.update(subscription_id, {
        cancel_at_period_end: true
      });
    }

    return {
      id: result.id,
      status: result.status,
      cancel_at_period_end: result.cancel_at_period_end,
      canceled_at: result.canceled_at
    };
  } catch (error) {
    console.error('Error canceling subscription:', error);
    throw new functions.https.HttpsError(
      'internal',
      'Failed to cancel subscription'
    );
  }
});

/**
 * Sync subscriptions between Stripe and Firestore
 */
export const syncSubscriptions = functions.https.onCall(async (data, context) => {
  // Verify authentication
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'User must be logged in to sync subscriptions'
    );
  }

  try {
    const { user_id } = data;

    // Verify that user_id matches authenticated user
    if (user_id !== context.auth.uid) {
      throw new functions.https.HttpsError(
        'permission-denied',
        'User ID does not match authenticated user'
      );
    }

    // Find customer for user
    const customerSnapshot = await db.collection('stripe_customers')
      .where('userId', '==', context.auth.uid)
      .limit(1)
      .get();

    if (customerSnapshot.empty) {
      // No customer, nothing to sync
      return { success: true, message: 'No subscriptions found for user' };
    }

    const customerId = customerSnapshot.docs[0].data().customerId;

    // Get subscriptions from Stripe
    const stripeSubscriptions = await stripe.subscriptions.list({
      customer: customerId,
      status: 'all',
      expand: ['data.default_payment_method']
    });

    // Start batch write to Firestore
    const batch = db.batch();

    // Process each subscription
    for (const subscription of stripeSubscriptions.data) {
      // Find existing subscription in Firestore
      const subscriptionSnapshot = await db.collection('subscriptions')
        .where('userId', '==', context.auth.uid)
        .where('providerSubscriptionId', '==', subscription.id)
        .limit(1)
        .get();

      const isActive = subscription.status === 'active' || subscription.status === 'trialing';
      const subscriptionStatus = isActive 
        ? (subscription.cancel_at_period_end ? 'ACTIVE_UNTIL_END' : 'ACTIVE')
        : subscription.status.toUpperCase();

      if (subscriptionSnapshot.empty) {
        // Create new subscription in Firestore
        const newSubscriptionRef = db.collection('subscriptions').doc();
        batch.set(newSubscriptionRef, {
          id: newSubscriptionRef.id,
          userId: context.auth.uid,
          type: subscription.metadata.subscription_type || 'BASIC',
          status: subscriptionStatus,
          paymentProvider: 'STRIPE',
          providerSubscriptionId: subscription.id,
          priceUsd: subscription.items.data[0].price.unit_amount ? subscription.items.data[0].price.unit_amount / 100 : 0,
          startDate: new Date(subscription.start_date * 1000),
          currentPeriodEnd: new Date(subscription.current_period_end * 1000),
          autoRenew: !subscription.cancel_at_period_end,
          canceledAt: subscription.canceled_at ? new Date(subscription.canceled_at * 1000) : null,
          createdAt: new Date(),
          updatedAt: new Date()
        });
      } else {
        // Update existing subscription
        const subscriptionRef = subscriptionSnapshot.docs[0].ref;
        batch.update(subscriptionRef, {
          status: subscriptionStatus,
          currentPeriodEnd: new Date(subscription.current_period_end * 1000),
          autoRenew: !subscription.cancel_at_period_end,
          canceledAt: subscription.canceled_at ? new Date(subscription.canceled_at * 1000) : null,
          updatedAt: new Date()
        });
      }
    }

    // Commit all updates
    await batch.commit();

    // Update user's premium status
    const hasActiveSubscription = stripeSubscriptions.data.some(sub => 
      (sub.status === 'active' || sub.status === 'trialing') && 
      sub.metadata.subscription_type !== 'BASIC'
    );

    await db.collection('users').doc(context.auth.uid).update({
      isPremium: hasActiveSubscription,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return { success: true, message: 'Subscriptions synced successfully' };
  } catch (error) {
    console.error('Error syncing subscriptions:', error);
    throw new functions.https.HttpsError(
      'internal',
      'Failed to sync subscriptions'
    );
  }
});

/**
 * Stripe webhook handler
 */
app.post('/stripe-webhook', async (req, res) => {
  const signature = req.headers['stripe-signature'];

  if (!signature || !webhookSecret) {
    console.error('Missing stripe-signature or webhook secret');
    return res.status(400).send('Missing signature');
  }

  let event;

  try {
    event = stripe.webhooks.constructEvent(
      (req as any).rawBody,
      signature,
      webhookSecret
    );
  } catch (err) {
    console.error('Webhook signature verification failed:', err);
    return res.status(400).send('Webhook signature verification failed');
  }

  // Handle different event types
  try {
    switch (event.type) {
      case 'payment_intent.succeeded':
        await handlePaymentIntentSucceeded(event.data.object);
        break;
      case 'payment_intent.payment_failed':
        await handlePaymentIntentFailed(event.data.object);
        break;
      case 'customer.subscription.created':
      case 'customer.subscription.updated':
      case 'customer.subscription.deleted':
        await handleSubscriptionUpdated(event.data.object);
        break;
      default:
        console.log(`Unhandled event type: ${event.type}`);
    }

    res.json({ received: true });
  } catch (error) {
    console.error(`Error handling webhook ${event.type}:`, error);
    res.status(500).send(`Error handling webhook: ${error.message}`);
  }
});

/**
 * Handle successful payment intent
 */
async function handlePaymentIntentSucceeded(paymentIntent: Stripe.PaymentIntent) {
  try {
    // Get user ID from payment intent metadata
    const userId = paymentIntent.metadata.user_id;
    if (!userId) {
      console.error('Payment intent has no user ID in metadata:', paymentIntent.id);
      return;
    }

    // Find payment in Firestore
    const paymentsSnapshot = await db.collection('payments')
      .where('providerPaymentId', '==', paymentIntent.id)
      .limit(1)
      .get();

    if (paymentsSnapshot.empty) {
      console.error('Payment not found in Firestore:', paymentIntent.id);
      return;
    }

    const paymentDoc = paymentsSnapshot.docs[0];
    const paymentRef = paymentDoc.ref;
    const payment = paymentDoc.data();

    // Update payment status
    await paymentRef.update({
      status: 'SUCCEEDED',
      providerTransactionId: paymentIntent.charges.data[0]?.id || null,
      receiptUrl: paymentIntent.charges.data[0]?.receipt_url || null,
      requiresAction: false,
      actionUrl: null,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    // If this is a subscription payment, update the subscription
    if (payment.subscriptionId) {
      const subscriptionRef = db.collection('subscriptions').doc(payment.subscriptionId);
      const subscriptionDoc = await subscriptionRef.get();
      
      if (subscriptionDoc.exists) {
        await subscriptionRef.update({
          status: 'ACTIVE',
          updatedAt: admin.firestore.FieldValue.serverTimestamp()
        });
      }
    }

    // Update user's premium status if needed (could be from direct payment or subscription)
    if (payment.type === 'SUBSCRIPTION' || payment.subscriptionId) {
      await db.collection('users').doc(userId).update({
        isPremium: true,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      });
    }

    // Send push notification to user
    try {
      const userDoc = await db.collection('users').doc(userId).get();
      const userData = userDoc.data();
      
      if (userData?.fcmToken) {
        await messaging.send({
          token: userData.fcmToken,
          notification: {
            title: 'Payment Successful',
            body: `Your payment of $${(paymentIntent.amount / 100).toFixed(2)} has been processed successfully.`
          },
          data: {
            type: 'payment_success',
            paymentId: paymentDoc.id,
            amount: (paymentIntent.amount / 100).toString()
          },
          android: {
            priority: 'high',
            notification: {
              channelId: 'payments'
            }
          }
        });
      }
    } catch (error) {
      console.error('Error sending payment success notification:', error);
    }
  } catch (error) {
    console.error('Error handling payment_intent.succeeded:', error);
    throw error;
  }
}

/**
 * Handle failed payment intent
 */
async function handlePaymentIntentFailed(paymentIntent: Stripe.PaymentIntent) {
  try {
    // Get user ID from payment intent metadata
    const userId = paymentIntent.metadata.user_id;
    if (!userId) {
      console.error('Payment intent has no user ID in metadata:', paymentIntent.id);
      return;
    }

    // Find payment in Firestore
    const paymentsSnapshot = await db.collection('payments')
      .where('providerPaymentId', '==', paymentIntent.id)
      .limit(1)
      .get();

    if (paymentsSnapshot.empty) {
      console.error('Payment not found in Firestore:', paymentIntent.id);
      return;
    }

    const paymentDoc = paymentsSnapshot.docs[0];
    const paymentRef = paymentDoc.ref;
    const payment = paymentDoc.data();

    // Update payment status
    await paymentRef.update({
      status: 'FAILED',
      requiresAction: false,
      actionUrl: null,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    // Send push notification to user
    try {
      const userDoc = await db.collection('users').doc(userId).get();
      const userData = userDoc.data();
      
      if (userData?.fcmToken) {
        await messaging.send({
          token: userData.fcmToken,
          notification: {
            title: 'Payment Failed',
            body: `Your payment of $${(paymentIntent.amount / 100).toFixed(2)} could not be processed.`
          },
          data: {
            type: 'payment_failed',
            paymentId: paymentDoc.id,
            amount: (paymentIntent.amount / 100).toString()
          },
          android: {
            priority: 'high',
            notification: {
              channelId: 'payments'
            }
          }
        });
      }
    } catch (error) {
      console.error('Error sending payment failed notification:', error);
    }
  } catch (error) {
    console.error('Error handling payment_intent.payment_failed:', error);
    throw error;
  }
}

/**
 * Handle subscription updates
 */
async function handleSubscriptionUpdated(subscription: Stripe.Subscription) {
  try {
    // Get customer
    const customer = await stripe.customers.retrieve(subscription.customer as string);
    const userId = customer.metadata.user_id;

    if (!userId) {
      console.error('Customer has no user ID in metadata:', customer.id);
      return;
    }

    // Find subscription in Firestore
    const subscriptionsSnapshot = await db.collection('subscriptions')
      .where('providerSubscriptionId', '==', subscription.id)
      .limit(1)
      .get();

    const isActive = subscription.status === 'active' || subscription.status === 'trialing';
    const subscriptionStatus = isActive 
      ? (subscription.cancel_at_period_end ? 'ACTIVE_UNTIL_END' : 'ACTIVE')
      : subscription.status.toUpperCase();

    if (subscriptionsSnapshot.empty) {
      // Create new subscription in Firestore
      const newSubscriptionRef = db.collection('subscriptions').doc();
      await newSubscriptionRef.set({
        id: newSubscriptionRef.id,
        userId: userId,
        type: subscription.metadata.subscription_type || 'BASIC',
        status: subscriptionStatus,
        paymentProvider: 'STRIPE',
        providerSubscriptionId: subscription.id,
        priceUsd: subscription.items.data[0].price.unit_amount ? subscription.items.data[0].price.unit_amount / 100 : 0,
        startDate: new Date(subscription.start_date * 1000),
        currentPeriodEnd: new Date(subscription.current_period_end * 1000),
        autoRenew: !subscription.cancel_at_period_end,
        canceledAt: subscription.canceled_at ? new Date(subscription.canceled_at * 1000) : null,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      });
    } else {
      // Update existing subscription
      const subscriptionRef = subscriptionsSnapshot.docs[0].ref;
      await subscriptionRef.update({
        status: subscriptionStatus,
        currentPeriodEnd: new Date(subscription.current_period_end * 1000),
        autoRenew: !subscription.cancel_at_period_end,
        canceledAt: subscription.canceled_at ? new Date(subscription.canceled_at * 1000) : null,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      });
    }

    // Update user's premium status
    if (subscription.status === 'active' && subscription.metadata.subscription_type !== 'BASIC') {
      await db.collection('users').doc(userId).update({
        isPremium: true,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      });

      // Send subscription activated notification
      sendSubscriptionNotification(userId, subscription, 'activated');
    } else if (subscription.status === 'canceled' || subscription.status === 'unpaid') {
      // Check if user has any other active subscriptions before removing premium status
      const activeSubscriptionsSnapshot = await db.collection('subscriptions')
        .where('userId', '==', userId)
        .where('status', '==', 'ACTIVE')
        .where('type', '!=', 'BASIC')
        .limit(1)
        .get();

      if (activeSubscriptionsSnapshot.empty) {
        await db.collection('users').doc(userId).update({
          isPremium: false,
          updatedAt: admin.firestore.FieldValue.serverTimestamp()
        });

        // Send subscription canceled notification
        sendSubscriptionNotification(userId, subscription, 'canceled');
      }
    }
  } catch (error) {
    console.error('Error handling subscription update:', error);
    throw error;
  }
}

/**
 * Send notification about subscription status
 */
async function sendSubscriptionNotification(
  userId: string, 
  subscription: Stripe.Subscription, 
  status: 'activated' | 'canceled' | 'expiring'
) {
  try {
    const userDoc = await db.collection('users').doc(userId).get();
    const userData = userDoc.data();
    
    if (!userData?.fcmToken) {
      return;
    }

    let title, body;
    const subscriptionType = subscription.metadata.subscription_type || 'Premium';

    if (status === 'activated') {
      title = 'Subscription Activated';
      body = `Your ${subscriptionType} subscription is now active. Enjoy premium features!`;
    } else if (status === 'canceled') {
      title = 'Subscription Canceled';
      body = `Your ${subscriptionType} subscription has been canceled.`;
    } else if (status === 'expiring') {
      title = 'Subscription Expiring Soon';
      body = `Your ${subscriptionType} subscription will expire soon. Renew to keep premium features.`;
    }

    await messaging.send({
      token: userData.fcmToken,
      notification: {
        title,
        body
      },
      data: {
        type: 'subscription_' + status,
        subscriptionId: subscription.id,
        subscriptionType: subscriptionType
      },
      android: {
        priority: 'high',
        notification: {
          channelId: 'subscriptions'
        }
      }
    });
  } catch (error) {
    console.error(`Error sending subscription ${status} notification:`, error);
  }
}

/**
 * Check for soon-to-expire subscriptions daily and send notifications
 */
export const checkExpiringSubscriptions = functions.pubsub.schedule('0 10 * * *') // Every day at 10:00 AM
  .timeZone('UTC')
  .onRun(async context => {
    try {
      const now = admin.firestore.Timestamp.now();
      const threeDaysLater = new Date();
      threeDaysLater.setDate(threeDaysLater.getDate() + 3);
      
      // Find subscriptions expiring in the next 3 days
      const expiringSubscriptionsSnapshot = await db.collection('subscriptions')
        .where('status', '==', 'ACTIVE')
        .where('autoRenew', '==', false)
        .where('currentPeriodEnd', '<=', threeDaysLater)
        .where('currentPeriodEnd', '>', now.toDate())
        .get();
      
      if (expiringSubscriptionsSnapshot.empty) {
        console.log('No expiring subscriptions found');
        return null;
      }
      
      console.log(`Found ${expiringSubscriptionsSnapshot.docs.length} expiring subscriptions`);
      
      // Send notifications for each expiring subscription
      for (const doc of expiringSubscriptionsSnapshot.docs) {
        const subscription = doc.data();
        
        // Get Stripe subscription for more details
        const stripeSubscription = await stripe.subscriptions.retrieve(subscription.providerSubscriptionId);
        
        // Send notification to user
        await sendSubscriptionNotification(subscription.userId, stripeSubscription, 'expiring');
      }
      
      return null;
    } catch (error) {
      console.error('Error checking expiring subscriptions:', error);
      return null;
    }
  });

// Export the Express app as a Firebase HTTP function
export const stripeWebhook = functions.https.onRequest(app);