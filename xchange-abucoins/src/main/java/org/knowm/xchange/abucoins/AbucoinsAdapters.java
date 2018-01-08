package org.knowm.xchange.abucoins;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.knowm.xchange.abucoins.dto.marketdata.AbucoinsOrderBook;
import org.knowm.xchange.abucoins.dto.marketdata.AbucoinsTicker;
import org.knowm.xchange.abucoins.dto.marketdata.AbucoinsTrade;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.dto.marketdata.Trades;
import org.knowm.xchange.dto.marketdata.Trades.TradeSortType;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Author: bryant_harris
 */

public class AbucoinsAdapters {
  private static Logger logger = LoggerFactory.getLogger(AbucoinsAdapters.class);
          
  protected static Date parseDate(final String rawDate) {

    String modified;
    if (rawDate.length() > 23) {
      modified = rawDate.substring(0, 23);
    } else if (rawDate.endsWith("Z")) {
      switch (rawDate.length()) {
      case 20:
        modified = rawDate.substring(0, 19) + ".000";
        break;
      case 22:
        modified = rawDate.substring(0, 21) + "00";
        break;
      case 23:
        modified = rawDate.substring(0, 22) + "0";
        break;
      default:
        modified = rawDate;
        break;
      }
    } else {
      switch (rawDate.length()) {
      case 19:
        modified = rawDate + ".000";
        break;
      case 21:
        modified = rawDate + "00";
        break;
      case 22:
        modified = rawDate + "0";
        break;
      default:
        modified = rawDate;
        break;
      }
    }
    try {
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
      dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      return dateFormat.parse(modified);
    } catch (ParseException e) {
      logger.warn("unable to parse rawDate={} modified={}", rawDate, modified, e);
      return null;
    }
  }

  /**
   * Adapts a AbucoinsTrade to a Trade Object
   *
   * @param trade        Abucoins trade object
   * @param currencyPair trade currencies
   * @return The XChange Trade
   */
  public static Trade adaptTrade(AbucoinsTrade trade, CurrencyPair currencyPair) {

    BigDecimal amount = trade.getSize();
    BigDecimal price = trade.getPrice();
    Date date = parseDate(trade.getTime());
    OrderType type = trade.getSide().equals("buy") ? OrderType.BID : OrderType.ASK;
    return new Trade(type, amount, currencyPair, price, date, trade.getTradeID());
  }

  /**
   * Adapts a AbucoinsTrade[] to a Trades Object
   *
   * @param cexioTrades  The Abucoins trade data returned by API
   * @param currencyPair trade currencies
   * @return The trades
   */
  public static Trades adaptTrades(AbucoinsTrade[] abucoinsTrades, CurrencyPair currencyPair) {

    List<Trade> tradesList = new ArrayList<>();
    long lastTradeId = 0;
    for (AbucoinsTrade trade : abucoinsTrades )
      tradesList.add( adaptTrade(trade, currencyPair));

    return new Trades(tradesList, lastTradeId, TradeSortType.SortByTimestamp);
  }

  /**
   * Adapts a AbucoinsTicker to a Ticker Object
   *
   * @param ticker       The exchange specific ticker
   * @param currencyPair The currency pair (e.g. BTC/USD)
   * @return The ticker
   */
  public static Ticker adaptTicker(AbucoinsTicker ticker, CurrencyPair currencyPair) {

    BigDecimal last = ticker.getPrice();
    BigDecimal bid = ticker.getBid();
    BigDecimal ask = ticker.getAsk();
    BigDecimal volume = ticker.getVolume();
    if ( ticker.getTime() == null )
      throw new RuntimeException("Null date for: " + ticker); 
    Date timestamp = parseDate(ticker.getTime());

    return new Ticker.Builder().currencyPair(currencyPair).last(last).bid(bid).ask(ask).volume(volume).timestamp(timestamp)
        .build();
  }

  /**
   * Adapts Cex.IO Depth to OrderBook Object
   *
   * @param depth        Cex.IO order book
   * @param currencyPair The currency pair (e.g. BTC/USD)
   * @return The XChange OrderBook
   */
  public static OrderBook adaptOrderBook(AbucoinsOrderBook abucoinsOrderBook, CurrencyPair currencyPair) {

    List<LimitOrder> asks = createOrders(currencyPair, OrderType.ASK, abucoinsOrderBook.getAsks());
    List<LimitOrder> bids = createOrders(currencyPair, OrderType.BID, abucoinsOrderBook.getBids());
    
    return new OrderBook(new Date(), asks, bids);
  }

  /**
   * Adapts AbucoinsBalanceInfo to Wallet
   *
   * @param cexIOBalanceInfo AbucoinsBalanceInfo balance
   * @return The account info
   *
  public static Wallet adaptWallet(AbucoinsBalanceInfo cexIOBalanceInfo) {

    List<Balance> balances = new ArrayList<>();
    for (String ccyName : cexIOBalanceInfo.getBalances().keySet()) {
      AbucoinsBalance cexIOBalance = cexIOBalanceInfo.getBalances().get(ccyName);
      balances.add(adaptBalance(new Currency(ccyName), cexIOBalance));
    }

    return new Wallet(balances);
  }

  public static Balance adaptBalance(Currency currency, AbucoinsBalance balance) {
    BigDecimal inOrders = balance.getOrders();
    BigDecimal frozen = inOrders == null ? BigDecimal.ZERO : inOrders;
    return new Balance(currency, null, balance.getAvailable(), frozen);
  }
*/
  public static List<LimitOrder> createOrders(CurrencyPair currencyPair, OrderType orderType, AbucoinsOrderBook.LimitOrder[] orders) {

    List<LimitOrder> limitOrders = new ArrayList<>();
    if (orders == null)
      return limitOrders;

    for (AbucoinsOrderBook.LimitOrder o : orders) {
      limitOrders.add(createOrder(currencyPair, o, orderType));
    }
    return limitOrders;
  }

  public static LimitOrder createOrder(CurrencyPair currencyPair, AbucoinsOrderBook.LimitOrder priceAndAmount, OrderType orderType) {

    return new LimitOrder(orderType, priceAndAmount.getPrice(), currencyPair, "", null, priceAndAmount.getSize()); //??
  }
/*
  public static void checkArgument(boolean argument, String msgPattern, Object... msgArgs) {

    if (!argument) {
      throw new IllegalArgumentException(MessageFormat.format(msgPattern, msgArgs));
    }
  }

  public static OpenOrders adaptOpenOrders(List<AbucoinsOrder> cexIOOrderList) {

    List<LimitOrder> limitOrders = new ArrayList<>();

    for (AbucoinsOrder cexIOOrder : cexIOOrderList) {
      OrderType orderType = cexIOOrder.getType() == AbucoinsOrder.Type.buy ? OrderType.BID : OrderType.ASK;
      String id = Long.toString(cexIOOrder.getId());
      limitOrders.add(new LimitOrder(orderType, cexIOOrder.getAmount(), cexIOOrder.getPending(),
          new CurrencyPair(cexIOOrder.getTradableIdentifier(), cexIOOrder.getTransactionCurrency()), id,
          DateUtils.fromMillisUtc(cexIOOrder.getTime()), cexIOOrder.getPrice()));
    }

    return new OpenOrders(limitOrders);

  }

  public static UserTrade adaptArchivedOrder(AbucoinsArchivedOrder cexIOArchivedOrder) {
    try {
      Date timestamp = fromISODateString(cexIOArchivedOrder.time);

      OrderType orderType = cexIOArchivedOrder.type.equals("sell") ? OrderType.ASK : OrderType.BID;
      BigDecimal originalAmount = cexIOArchivedOrder.amount;
      CurrencyPair currencyPair = new CurrencyPair(cexIOArchivedOrder.symbol1, cexIOArchivedOrder.symbol2);
      BigDecimal price = cexIOArchivedOrder.price;
      String id = cexIOArchivedOrder.id;
      String orderId = cexIOArchivedOrder.orderId;

      Currency feeCcy = cexIOArchivedOrder.feeCcy == null ? null : Currency.getInstance(cexIOArchivedOrder.feeCcy);
      BigDecimal fee = cexIOArchivedOrder.feeValue;

      return new UserTrade(orderType, originalAmount, currencyPair, price, timestamp, id, orderId, fee, feeCcy);
    } catch (InvalidFormatException e) {
      throw new IllegalStateException("Cannot format date " + cexIOArchivedOrder.time, e);
    }
  }

  public static Order adaptOrder(AbucoinsOpenOrder cexIOOrder) {
    OrderType orderType = cexIOOrder.type.equals("sell") ? OrderType.ASK : OrderType.BID;
    BigDecimal originalAmount = new BigDecimal(cexIOOrder.amount);
    CurrencyPair currencyPair = new CurrencyPair(cexIOOrder.symbol1, cexIOOrder.symbol2);
    Date timestamp = new Date(cexIOOrder.time);
    BigDecimal limitPrice = new BigDecimal(cexIOOrder.price);

    return new LimitOrder(orderType, originalAmount, currencyPair, cexIOOrder.orderId, timestamp, limitPrice);
  }
  */
}
