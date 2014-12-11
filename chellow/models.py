from chellow import app
from flask.ext.sqlalchemy import SQLAlchemy
from sqlalchemy import (
    Column, Integer, String, Boolean, Text, Numeric, Enum, DateTime,
    ForeignKey)
from sqlalchemy.orm import relationship
from sqlalchemy.schema import UniqueConstraint
import datetime
import pytz
import ast

db = SQLAlchemy(app)


class UserException(Exception):
    pass


def set_read_write():
    db.session.execute("rollback")
    db.session.execute(
        "set transaction isolation level serializable read write")


class PersistentClass():
    id = Column(Integer, primary_key=True)


class VoltageLevel(db.Model, PersistentClass):

    __tablename__ = "voltage_level"
    code = Column(String, unique=True, nullable=False)
    name = Column(String, unique=True, nullable=False)
    llfcs = relationship('Llfc', backref='voltage_level')

    @staticmethod
    def get_by_code(sess, code):
        vl = sess.query(VoltageLevel).filter_by(code=code).first()
        if vl is None:
            raise UserException(
                "There is no voltage level with the code '" + code + "'.")
        return vl


class Site(db.Model, PersistentClass):
    __tablename__ = 'site'
    id = Column('id', Integer, primary_key=True)
    code = Column(String, unique=True, nullable=False)
    name = Column(String, unique=True, nullable=False)
    site_eras = relationship('SiteEra', backref='site')
    snags = relationship('Snag', backref='site')

    def __init__(self, code, name):
        self.update(code, name)


class MarketRole(db.Model, PersistentClass):
    __tablename__ = 'market_role'
    id = Column('id', Integer, primary_key=True)
    code = Column(String(length=1), unique=True, nullable=False)
    description = Column(String, nullable=False, unique=True)
    contracts = relationship('Contract', backref='market_role')
    parties = relationship('Party', backref='market_role')


class Participant(db.Model, PersistentClass):
    __tablename__ = 'participant'
    id = Column('id', Integer, primary_key=True)
    code = Column(String, unique=True, nullable=False)
    name = Column(String, nullable=False)
    parties = relationship('Party', backref='participant')


class Party(db.Model, PersistentClass):
    __tablename__ = 'party'
    id = Column('id', Integer, primary_key=True)
    market_role_id = Column(Integer, ForeignKey('market_role.id'))
    participant_id = Column(Integer, ForeignKey('participant.id'))
    name = Column(String, nullable=False)
    valid_from = Column(DateTime, nullable=False)
    valid_to = Column(DateTime)
    users = relationship('User', backref='party')
    dno_code = Column(String)
    contracts = relationship('Contract', backref='party')
    mtcs = relationship('Mtc', backref='dno')
    llfcs = relationship('Llfc', backref='dno')


class Source(db.Model, PersistentClass):
    __tablename__ = "source"
    id = Column('id', Integer, primary_key=True)
    code = Column(String, unique=True, nullable=False)
    name = Column(String, unique=True, nullable=False)
    supplies = relationship('Supply', backref='source')


class GeneratorType(db.Model, PersistentClass):
    __tablename__ = 'generator_type'
    code = Column(String, unique=True, nullable=False)
    description = Column(String, unique=True, nullable=False)
    supplies = relationship('Supply', backref='generator_type')


class GspGroup(db.Model, PersistentClass):
    __tablename__ = 'gsp_group'
    id = Column('id', Integer, primary_key=True)
    code = Column(String, unique=True, nullable=False)
    description = Column(String, unique=True, nullable=False)
    supplies = relationship('Supply', backref='gsp_group')


class Contract(db.Model, PersistentClass):
    __tablename__ = 'contract'
    id = Column('id', Integer, primary_key=True)
    is_core = Column(Boolean, nullable=False)
    name = Column(String, nullable=False)
    charge_script = Column(Text, nullable=False)
    properties = Column(Text, nullable=False)
    state = Column(Text, nullable=False)
    market_role_id = Column(Integer, ForeignKey('market_role.id'))
    __table_args__ = (UniqueConstraint('name', 'market_role_id'),)
    rate_scripts = relationship(
        "RateScript", back_populates="contract",
        primaryjoin="Contract.id==RateScript.contract_id")
    batches = relationship('Batch', backref='contract')
    supplies = relationship('Supply', backref='dno_contract')
    party_id = Column(Integer, ForeignKey('party.id'))

    start_rate_script_id = Column(
        Integer, ForeignKey(
            'rate_script.id', use_alter=True,
            name='contract_start_rate_script_id_fkey'))
    finish_rate_script_id = Column(
        Integer, ForeignKey(
            'rate_script.id', use_alter=True,
            name='contract_finish_rate_script_id_fkey'))

    start_rate_script = relationship(
        "RateScript",
        primaryjoin="RateScript.id==Contract.start_rate_script_id")
    finish_rate_script = relationship(
        "RateScript",
        primaryjoin="RateScript.id==Contract.finish_rate_script_id")

    @staticmethod
    def get_non_core_by_name(name):
        return Contract.get_by_role_code_name('Z', name)

    @staticmethod
    def get_dno_by_id(oid):
        return Contract.get_by_role_code_id('R', oid)

    @staticmethod
    def get_dno_by_name(name):
        cont = Contract.find_by_role_code_name('R', name)
        if cont is None:
            raise UserException(
                "There isn't a DNO contract with the code '" + name + "'.")
        return cont

    @staticmethod
    def get_hhdc_by_id(oid):
        return Contract.get_by_role_code_id('C', oid)

    @staticmethod
    def get_by_role_code_name(role_code, name):
        cont = Contract.find_by_role_code_name(role_code, name)
        if cont is None:
            raise UserException(
                "There isn't a contract with the role code '" + role_code +
                "' and name '" + name + "'.")
        return cont

    @staticmethod
    def find_by_role_code_name(role_code, name):
        return Contract.query.join(MarketRole).filter(
            MarketRole.code == role_code, Contract.name == name).first()

    @staticmethod
    def find_by_role_code_id(role_code, oid):
        return Contract.query.join(MarketRole).filter(
            MarketRole.code == role_code, Contract.id == oid).first()

    @staticmethod
    def get_by_role_code_id(role_code, oid):
        cont = Contract.find_by_role_code_id(role_code, oid)
        if cont is None:
            raise UserException(
                "There isn't a contract with the role code '" + role_code +
                "' and id '" + str(oid) + "'.")
        return cont

    @staticmethod
    def insert_non_core(
            is_core, name, charge_script, properties, start_date, finish_date,
            rate_script):
        return Contract.insert(
            is_core, name, Participant.get_by_code('CALB'), 'Z', charge_script,
            properties, start_date, finish_date, rate_script)

    @staticmethod
    def insert(
            is_core, name, participant, role_code, charge_script, properties,
            start_date, finish_date, rate_script):
        party = Party.get_by_participant_id_role_code(
            participant.id, role_code)
        contract = Contract(is_core, name, party, charge_script, properties)
        db.session.add(contract)
        db.session.flush()
        rscript = contract.insert_rate_script(start_date, rate_script)
        contract.update_rate_script(
            rscript, start_date, finish_date, rate_script)
        return contract

    def make_properties(self):
        return eval(self.properties, {'datetime': datetime.datetime})


class RateScript(db.Model, PersistentClass):
    __tablename__ = "rate_script"
    id = Column('id', Integer, primary_key=True)
    contract_id = Column(Integer, ForeignKey('contract.id'))
    contract = relationship(
        "Contract", back_populates="rate_scripts",
        primaryjoin="Contract.id==RateScript.contract_id")
    start_date = Column(DateTime, nullable=False)
    finish_date = Column(DateTime, nullable=True)
    script = Column(Text, nullable=False)


class Pc(db.Model, PersistentClass):
    __tablename__ = 'pc'
    id = Column('id', Integer, primary_key=True)
    code = Column(String, unique=True, nullable=False)
    name = Column(String, unique=True, nullable=False)
    eras = relationship('Era', backref='pc')


class MeterType(db.Model, PersistentClass):
    __tablename__ = 'meter_type'
    id = Column('id', Integer, primary_key=True)
    code = Column(String, unique=True, nullable=False)
    description = Column(String, unique=True, nullable=False)
    valid_from = Column(DateTime, nullable=False)
    valid_to = Column(DateTime)
    mtcs = relationship('Mtc', backref='meter_type')


class MeterPaymentType(db.Model, PersistentClass):
    __tablename__ = 'meter_payment_type'
    id = Column('id', Integer, primary_key=True)
    code = Column(String, unique=True, nullable=False)
    description = Column(String, unique=True, nullable=False)
    valid_from = Column(DateTime)
    valid_to = Column(DateTime)
    mtcs = relationship('Mtc', backref='meter_payment_type')


class Mtc(db.Model, PersistentClass):
    __tablename__ = 'mtc'
    id = Column('id', Integer, primary_key=True)
    dno_id = Column(Integer, ForeignKey('party.id'))
    code = Column(String, nullable=False)
    description = Column(String, nullable=False)
    has_related_metering = Column(Boolean, nullable=False)
    has_comms = Column(Boolean, nullable=False)
    is_hh = Column(Boolean, nullable=False)
    meter_type_id = Column(Integer, ForeignKey('meter_type.id'))
    meter_payment_type_id = Column(
        Integer, ForeignKey('meter_payment_type.id'))
    tpr_count = Column(Integer)
    valid_from = Column(DateTime, nullable=False)
    valid_to = Column(DateTime)
    eras = relationship('Era', backref='mtc')
    __table_args__ = (UniqueConstraint('dno_id', 'code'),)


class Supply(db.Model, PersistentClass):
    __tablename__ = 'supply'
    id = Column('id', Integer, primary_key=True)
    name = Column(String, nullable=False)
    note = Column(Text, nullable=False)
    source_id = Column(Integer, ForeignKey('source.id'), nullable=False)
    generator_type_id = Column(Integer, ForeignKey('generator_type.id'))
    gsp_group_id = Column(Integer, ForeignKey('gsp_group.id'), nullable=False)
    dno_contract_id = Column(
        Integer, ForeignKey('contract.id'), nullable=False)
    eras = relationship('Era', backref='supply')
    bills = relationship('Bill', backref='supply')


class Cop(db.Model, PersistentClass):
    __tablename__ = 'cop'
    id = Column('id', Integer, primary_key=True)
    code = Column(String, unique=True, nullable=False)
    description = Column(String, unique=True, nullable=False)
    eras = relationship('Era', backref='cop')


class Ssc(db.Model, PersistentClass):
    __tablename__ = 'ssc'
    id = Column('id', Integer, primary_key=True)
    code = Column(String, nullable=False)
    description = Column(String)
    is_import = Column(Boolean)
    valid_from = Column(DateTime, nullable=False)
    valid_to = Column(DateTime)
    measurement_requirements = relationship(
        'MeasurementRequirement', backref='ssc')
    eras = relationship('Era', backref='ssc')


class Llfc(db.Model, PersistentClass):
    __tablename__ = 'llfc'
    id = Column('id', Integer, primary_key=True)
    dno_id = Column(Integer, ForeignKey('party.id'))
    code = Column(String, nullable=False)
    description = Column(String)
    voltage_level_id = Column(Integer, ForeignKey('voltage_level.id'))
    is_substation = Column(Boolean, nullable=False)
    is_import = Column(Boolean, nullable=False)
    valid_from = Column(DateTime, nullable=False)
    valid_to = Column(DateTime)


class Era(db.Model, PersistentClass):
    __tablename__ = "era"
    id = Column('id', Integer, primary_key=True)
    supply_id = Column(Integer, ForeignKey('supply.id'), nullable=False)
    site_eras = relationship('SiteEra', backref='era')
    start_date = Column(DateTime, nullable=False)
    finish_date = Column(DateTime)
    mop_contract_id = Column(
        Integer, ForeignKey('contract.id'), nullable=False)
    mop_contract = relationship(
        "Contract", primaryjoin="Contract.id==Era.mop_contract_id")
    mop_account = Column(String, nullable=False)
    hhdc_contract_id = Column(
        Integer, ForeignKey('contract.id'), nullable=False)
    hhdc_contract = relationship(
        "Contract", primaryjoin="Contract.id==Era.hhdc_contract_id")
    hhdc_account = Column(String)
    msn = Column(String)
    pc_id = Column(Integer, ForeignKey('pc.id'), nullable=False)
    mtc_id = Column(Integer, ForeignKey('mtc.id'), nullable=False)
    cop_id = Column(Integer, ForeignKey('cop.id'), nullable=False)
    ssc_id = Column(Integer, ForeignKey('ssc.id'))
    imp_mpan_core = Column(String)
    imp_llfc_id = Column(Integer, ForeignKey('llfc.id'))
    imp_llfc = relationship("Llfc", primaryjoin="Llfc.id==Era.imp_llfc_id")
    imp_supplier_contract_id = Column(Integer, ForeignKey('contract.id'))
    imp_supplier_contract = relationship(
        "Contract", primaryjoin="Contract.id==Era.imp_supplier_contract_id")
    imp_supplier_account = Column(String)
    imp_sc = Column(Integer)
    exp_mpan_core = Column(String)
    exp_llfc_id = Column(Integer, ForeignKey('llfc.id'))
    exp_llfc = relationship("Llfc", primaryjoin="Llfc.id==Era.exp_llfc_id")
    exp_supplier_contract_id = Column(Integer, ForeignKey('contract.id'))
    exp_supplier_contract = relationship(
        "Contract", primaryjoin="Contract.id==Era.exp_supplier_contract_id")
    exp_supplier_account = Column(String)
    exp_sc = Column(Integer)
    channels = relationship('Channel', backref='era')


class Channel(db.Model, PersistentClass):
    __tablename__ = 'channel'
    id = Column('id', Integer, primary_key=True)
    era_id = Column(Integer, ForeignKey('era.id'))
    imp_related = Column(Boolean, nullable=False)
    channel_type = Column(
        Enum('ACTIVE', 'REACTIVE_IMP', 'REACTIVE_EXP', name='channel_type'),
        nullable=False)
    hh_data = relationship('HhDatum', backref='channel')
    snag = relationship('Snag', backref='channel')
    __table_args__ = (
        UniqueConstraint('era_id', 'imp_related', 'channel_type'),)


class Snag(db.Model, PersistentClass):
    __tablename__ = 'snag'
    id = Column('id', Integer, primary_key=True)
    site_id = Column(Integer, ForeignKey('site.id'))
    channel_id = Column(Integer, ForeignKey('channel.id'))
    date_created = Column(DateTime, nullable=False)
    is_ignored = Column(Boolean, nullable=False)
    description = Column(String, nullable=False)
    start_date = Column(DateTime, nullable=False)
    finish_date = Column(DateTime, nullable=False)


class ReadType(db.Model, PersistentClass):
    __tablename__ = 'read_type'
    id = Column('id', Integer, primary_key=True)
    code = Column(String, unique=True, nullable=False)
    description = Column(String, unique=True, nullable=False)


class Batch(db.Model, PersistentClass):
    __tablename__ = 'batch'
    id = Column('id', Integer, primary_key=True)
    contract_id = Column(Integer, ForeignKey('contract.id'), nullable=False)
    reference = Column(String, nullable=False, unique=True)
    description = Column(String, nullable=False, unique=True)
    bills = relationship('Bill', backref='batch')


class BillType(db.Model, PersistentClass):
    __tablename__ = 'bill_type'
    id = Column('id', Integer, primary_key=True)
    code = Column(String, unique=True, nullable=False)
    description = Column(String, unique=True, nullable=False)
    bills = relationship('Bill', backref='bill_type')


class Bill(db.Model, PersistentClass):
    __tablename__ = 'bill'
    id = Column('id', Integer, primary_key=True)
    batch_id = Column(Integer, ForeignKey('batch.id'), nullable=False)
    supply_id = Column(Integer, ForeignKey('supply.id'), nullable=False)
    issue_date = Column(DateTime, nullable=False)
    start_date = Column(DateTime, nullable=False)
    finish_date = Column(DateTime, nullable=False)
    net = Column(Numeric, nullable=False)
    vat = Column(Numeric, nullable=False)
    gross = Column(Numeric, nullable=False)
    account = Column(String, nullable=False)
    reference = Column(String, nullable=False)
    bill_type_id = Column(Integer, ForeignKey('bill_type.id'))
    breakdown = Column(String, nullable=False)
    kwh = Column(Numeric, nullable=False)
    reads = relationship('RegisterRead', backref='bill')


class Tpr(db.Model, PersistentClass):
    __tablename__ = 'tpr'
    id = Column('id', Integer, primary_key=True)
    code = Column(String, unique=True, nullable=False)
    is_teleswitch = Column(Boolean, nullable=False)
    is_gmt = Column(Boolean, nullable=False)
    clock_intervals = relationship('ClockInterval', backref='tpr')
    measurement_requirements = relationship(
        'MeasurementRequirement', backref='tpr')
    register_reads = relationship('RegisterRead', backref='tpr')


class RegisterRead(db.Model, PersistentClass):
    __tablename__ = 'register_read'
    id = Column('id', Integer, primary_key=True)
    bill_id = Column(Integer, ForeignKey('bill.id'), nullable=False)
    msn = Column(String, nullable=False)
    mpan_str = Column(String, nullable=False)
    coefficient = Column(Numeric, nullable=False)
    units = Column(String, nullable=False)
    tpr_id = Column(Integer, ForeignKey('tpr.id'))
    previous_date = Column(DateTime, nullable=False)
    previous_value = Column(Numeric, nullable=False)
    previous_type_id = Column(Integer, ForeignKey('read_type.id'))
    previous_type = relationship(
        "ReadType", primaryjoin="ReadType.id==RegisterRead.previous_type_id")
    present_date = Column(DateTime, nullable=False)
    present_value = Column(Numeric, nullable=False)
    present_type_id = Column(Integer, ForeignKey('read_type.id'))
    present_type = relationship(
        "ReadType", primaryjoin="ReadType.id==RegisterRead.present_type_id")


class UserRole(db.Model, PersistentClass):
    __tablename__ = 'user_role'
    id = Column('id', Integer, primary_key=True)
    code = Column(String, unique=True, nullable=False)
    users = relationship('User', backref='user_role')


class User(db.Model, PersistentClass):
    __tablename__ = 'user'
    id = Column(Integer, primary_key=True)
    email_address = Column(String, unique=True, nullable=False)
    password_digest = Column(String, nullable=False)
    user_role_id = Column(Integer, ForeignKey('user_role.id'))
    party_id = Column(Integer, ForeignKey('party.id'))


class ClockInterval(db.Model, PersistentClass):
    __tablename__ = 'clock_interval'
    id = Column('id', Integer, primary_key=True)
    tpr_id = Column(Integer, ForeignKey('tpr.id'))
    day_of_week = Column(Integer, nullable=False)
    start_day = Column(Integer, nullable=False)
    start_month = Column(Integer, nullable=False)
    end_day = Column(Integer, nullable=False)
    end_month = Column(Integer, nullable=False)
    start_hour = Column(Integer, nullable=False)
    start_minute = Column(Integer, nullable=False)
    end_hour = Column(Integer, nullable=False)
    end_minute = Column(Integer, nullable=False)


class MeasurementRequirement(db.Model, PersistentClass):
    __tablename__ = 'measurement_requirement'
    id = Column('id', Integer, primary_key=True)
    ssc_id = Column(Integer, ForeignKey('ssc.id'))
    tpr_id = Column(Integer, ForeignKey('tpr.id'))


class SiteEra(db.Model, PersistentClass):
    __tablename__ = 'site_era'
    id = Column('id', Integer, primary_key=True)
    site_id = Column(Integer, ForeignKey('site.id'))
    era_id = Column(Integer, ForeignKey('era.id'))
    is_physical = Column(Boolean, nullable=False)


class HhDatum(db.Model, PersistentClass):
    __tablename__ = 'hh_datum'
    id = Column('id', Integer, primary_key=True)
    channel_id = Column(Integer, ForeignKey('channel.id'))
    start_date = Column(DateTime, nullable=False)
    value = Column(Numeric, nullable=False)
    status = Column(String, nullable=False)
    last_modified = Column(DateTime, nullable=False)
    __table_args__ = (UniqueConstraint('channel_id', 'start_date'),)


class Configuration(db.Model, PersistentClass):
    id = Column('id', Integer, primary_key=True)
    properties = Column(Text, nullable=False)
    core_report_id = Column('core_report_id', Integer)
    user_report_id = Column('user_report_id', Integer)

    def next_core_report_id(self):
        self.core_report_id += 2
        return self.core_report_id

    def next_user_report_id(self):
        self.user_report_id += 2
        return self.user_report_id


class Report(db.Model, PersistentClass):
    __tablename__ = 'report'
    id = Column('id', Integer, primary_key=True)
    name = Column(String, unique=True, nullable=False)
    script = Column(Text, nullable=False)
    template = Column(Text, nullable=False)

    def __init__(self, id, is_core, name, script, template):
        configuration = Configuration.query.first()

        if id is None:
            if is_core:
                id = configuration.next_core_report_id()
            else:
                id = configuration.next_user_report_id()

        else:
            if is_core:
                if id > configuration.core_report_id:
                    configuration.core_report_id = id
            else:
                if id > configuration.user_report_id:
                    configuration.user_report_id(id)

        is_odd = id % 2 == 1
        if is_odd != is_core:
            raise UserException(
                "The ids of core reports must be odd, those of user reports"
                "must be even. Report id " + id + ", Is Odd? " + is_odd +
                ", is core? " + is_core + ".")
        self.id = id
        self.update(name, script, template)

    def is_core(self):
        return self.id % 2 == 1

    def update(self, name, script, template):
        self.name = name
        try:
            ast.parse(script)
            self.script = script
        except SyntaxError, e:
            raise UserException(e)

        if template is not None and len(template.strip()) == 0:
            template = None

        self.template = template


def parse_hh_date(date_str):
    date_str = date_str.strip()
    if len(date_str) == 0:
        return None
    else:
        return datetime.datetime.strptime(date_str, "%Y-%m-%d %H:%M"). \
            replace(tzinfo=pytz.utc)