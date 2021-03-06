#!/usr/bin/env python

from setuptools import setup
import versioneer
import time
import sys


if "--test" in sys.argv:
    versioneer.tstamp = str(int(time.time()))
    sys.argv.remove("--test")


setup(
    name='chellow',
    version=versioneer.get_version(),
    description='Web Application for checking UK utility bills.',
    author='Tony Locke',
    author_email='tlocke@tlocke.org.uk',
    url='https://github.com/WessexWater/chellow',
    cmdclass=versioneer.get_cmdclass(),
    packages=[
        'chellow', 'chellow.reports', 'amazon', 'amazon.ion'],
    package_data={'chellow': [
        'non_core_contracts/*/*.py',
        'non_core_contracts/*/rate_scripts/*.py',
        'non_core_contracts/*/rate_scripts/*.ion',
        'dno_contracts/*/*.py',
        'dno_contracts/*/rate_scripts/*.py',
        'mdd/*.csv',
        'templates/*.html',
        'templates/css/*.css',
        'templates/js/*.js',
        'templates/*.css',
        'static/images/**',
        'static/css/**',
        'static/js/**',
        'static/font-awesome-4.6.3/**',
        'static/font-awesome-4.6.3/css/**',
        'static/font-awesome-4.6.3/fonts/**',
        'static/font-awesome-4.6.3/less/**',
        'static/font-awesome-4.6.3/scss/**',
        'nationalgrid/*',
        'elexonportal/*',
        'rate_scripts/*/*.ion']},
    install_requires=[
        'odio==0.0.15',
        'pg8000==1.10.6',
        'Flask==0.12',
        'SQLAlchemy==1.1.4',
        'openpyxl==2.4.0',
        'python-dateutil==2.4.2',
        'pytz==2016.10',
        'xlrd==0.9.4',
        'ftputil==3.2',
        'requests==2.10.0',
        'waitress==1.0.1',
        'pep3143daemon==0.0.6',
        'pip>=9.0.1',
        'pysftp==0.2.9',
        'pympler==0.5',
        'psutil==5.0.1'],
    data_files=[('config', ['config/chellow.conf'])],
    entry_points={
        'console_scripts': [
            'chellow = chellow.commands:chellow_command',
            'chellow_test_setup = chellow.commands:chellow_test_setup']},
    scripts=['bin/chellow_service_monitor.sh', 'bin/chellow_start.sh'])
