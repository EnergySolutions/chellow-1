import datetime
import pytz
import chellow.scenario
from chellow.utils import HH
from chellow.models import Session, Contract
import chellow.computer


sess = None
try:
    sess = Session()
    g_ccl_contract_id = Contract.get_non_core_by_name(sess, 'g_ccl').id
finally:
    if sess is not None:
        sess.close()


create_future_func = chellow.scenario.make_create_future_func_simple(
    'g_ccl', ['g_ccl_rate'])

THRESHOLD = 4397


def vb(data_source):
    rate_set = data_source.rate_sets['ccl_rate']

    if data_source.g_supply.find_g_era_at(
            data_source.sess, data_source.finish_date + HH) is None:
        sup_end = data_source.finish_date
    else:
        sup_end = None

    try:
        cache = data_source.caches['g_ccl']
    except:
        data_source.caches['g_ccl'] = {}
        cache = data_source.caches['g_ccl']

        try:
            future_funcs = data_source.caches['future_funcs']
        except KeyError:
            future_funcs = {}
            data_source.caches['future_funcs'] = future_funcs

        try:
            future_funcs[g_ccl_contract_id]
        except KeyError:
            future_funcs[g_ccl_contract_id] = {
                'start_date': None, 'func': create_future_func(1, 0)}

    if data_source.g_bill is None:
        for hh in data_source.hh_data:
            if hh['utc-is-month-end'] or hh['start-date'] == sup_end:
                month_finish = hh['start-date']
                kwh = 0
                gbp = 0
                month_start = datetime.datetime(
                    month_finish.year, month_finish.month, 1, tzinfo=pytz.utc)

                for ds in chellow.computer.get_data_sources(
                        data_source, month_start, month_finish):
                    for datum in ds.hh_data:
                        try:
                            rate = cache[datum['start_date']]
                        except KeyError:
                            cache[datum['start_date']] = data_source.rate(
                                g_ccl_contract_id, datum['start_date'],
                                'ccl_gbp_per_kwh')
                            rate = cache[datum['start_date']]

                        rate_set.add(rate)
                        kwh += datum['kwh']
                        gbp += datum['kwh'] * rate

                if kwh > THRESHOLD:
                    hh['ccl_kwh'] = kwh
                    hh['ccl_gbp'] = gbp

    elif data_source.is_last_g_bill_gen:
        kwh = 0
        gbp = 0
        for ds in chellow.computer.get_data_sources(
                data_source, data_source.g_bill_start,
                data_source.g_bill_finish):
            for hh in ds.hh_data:
                try:
                    rate = cache[hh['start_date']]
                except KeyError:
                    cache[hh['start_date']] = data_source.rate(
                        g_ccl_contract_id, hh['start_date'], 'ccl_gbp_per_kwh')
                    rate = cache[hh['start_date']]

                rate_set.add(rate)
                kwh += hh['kwh']
                gbp += hh['kwh'] * rate

        if kwh > THRESHOLD:
            data_source.hh_data[-1]['ccl_kwh'] = kwh
            data_source.hh_data[-1]['ccl_gbp'] = gbp