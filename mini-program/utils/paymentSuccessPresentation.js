export function getPaymentSuccessPresentation(deliveryStatus) {
  if (deliveryStatus === 1 || deliveryStatus === "1") {
    return {
      arrivalKey: "success.immediateArrival",
      messageKey: "success.immediatePreparing",
    };
  }

  if (deliveryStatus === 0 || deliveryStatus === "0") {
    return {
      arrivalKey: "success.scheduledArrival",
      messageKey: "success.scheduledPreparing",
    };
  }

  return {
    arrivalKey: "success.unknownArrival",
    messageKey: "success.unknownPreparing",
  };
}
